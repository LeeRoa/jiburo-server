package com.jiburo.server.domain.image.service;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.setting.AppSettingService;
import com.jiburo.server.global.setting.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {

    private final S3Presigner s3Presigner;
    private final ImageMetaRepository imageMetaRepository;
    private final UserRepository userRepository;
    private final AppSettingService appSettingService;

    private static final long MB = 1024L * 1024;
    private static final long GB = 1024L * MB;

    @Value("${cloud.cloudflare.r2.bucket}")
    private String bucketName;

    @Override
    public PresignedUrlResponseDto createPresignedUrl(UUID userId, PresignedUrlRequestDto request) {

        if (!request.fileCode().isExtensionAllowed(request.extension())) {
            throw new JiburoException(ErrorCode.INVALID_FILE_FORMAT);
        }

        // 파일 1개 최대 크기 (관리자 설정: 이미지/동영상 별도)
        SettingKey maxSizeKey = request.fileCode().isVideo()
                ? SettingKey.UPLOAD_MAX_VIDEO_SIZE_MB
                : SettingKey.UPLOAD_MAX_IMAGE_SIZE_MB;
        if (request.fileSize() > appSettingService.getInt(maxSizeKey) * MB) {
            throw new JiburoException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 전체 저장 용량 한도 (관리자 설정)
        long capacityBytes = appSettingService.getInt(SettingKey.UPLOAD_TOTAL_CAPACITY_GB) * GB;
        Long currentTotalSize = imageMetaRepository.sumTotalFileSize();
        if (currentTotalSize + request.fileSize() > capacityBytes) {
            throw new JiburoException(ErrorCode.STORAGE_CAPACITY_EXCEEDED);
        }

        long expirationMinutes = appSettingService.getInt(SettingKey.UPLOAD_PRESIGNED_EXPIRATION_MINUTES);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));

        String basePath = request.fileCode().getFolderPath();

        String fileKey = UUID.randomUUID() + "." + request.extension();
        String objectKey = basePath + "/" + userId + "/" + fileKey;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentLength(request.fileSize())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String url = presignedRequest.url().toString();

        ImageMeta imageMeta = request.toEntity(user, objectKey);
        imageMetaRepository.save(imageMeta);

        return new PresignedUrlResponseDto(url, objectKey);
    }

    @Override
    @Transactional
    public void completeUpload(UUID userId, String fileKey) {
        // 1. DB에서 파일 경로로 메타데이터 조회
        ImageMeta imageMeta = imageMetaRepository.findByFileKeyAndUseYnTrue(fileKey)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_FILE_FORMAT));

        // 2. 권한 검증 (본인이 발급받은 URL의 파일인지 확인)
        if (!imageMeta.getUser().getId().equals(userId)) {
            throw new JiburoException(ErrorCode.POST_ACCESS_DENIED);
        }

        // 3. 이미 완료된 파일인지 확인
        if (ImageStatus.COMPLETED.equals(imageMeta.getStatusCode())) {
            return;
        }

        // 4. 상태를 COMPLETED로 업데이트
        imageMeta.updateStatus(ImageStatus.COMPLETED);
    }
}
