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
    private static final long MAX_CAPACITY_BYTES = 10L * 1024 * 1024 * 1024; // 10GB

    @Value("${cloud.cloudflare.r2.bucket}")
    private String bucketName;

    @Value("${cloud.cloudflare.r2.presigned.expiration-minutes}")
    private long expirationMinutes;

    @Override
    public PresignedUrlResponseDto createPresignedUrl(UUID userId, PresignedUrlRequestDto request) {

        if (!request.fileCode().isExtensionAllowed(request.extension())) {
            throw new JiburoException(ErrorCode.INVALID_FILE_FORMAT);
        }

        Long currentTotalSize = imageMetaRepository.sumTotalFileSize();
        if (currentTotalSize + request.fileSize() > MAX_CAPACITY_BYTES) {
            throw new IllegalStateException("스토리지 전체 무료 용량(10GB)을 초과할 수 없습니다.");
        }

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
