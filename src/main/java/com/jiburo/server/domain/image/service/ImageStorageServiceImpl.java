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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {

    private final S3Presigner s3Presigner;
    private final ImageMetaRepository imageMetaRepository;
    private final UserRepository userRepository;
    private final AppSettingService appSettingService;

    private static final long MB = 1024L * 1024;
    private static final long GB = 1024L * MB;

    /**
     * presigned URL 만료 후에도 업로드가 진행 중일 수 있는 여유 시간.
     * 발급 후 (URL 유효 시간 + 이 시간)이 지난 PENDING은 업로드될 수 없는 것으로 보고 용량 계산에서 빼고 정리한다.
     */
    static final Duration PENDING_GRACE = Duration.ofHours(1);

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

        long expirationMinutes = appSettingService.getInt(SettingKey.UPLOAD_PRESIGNED_EXPIRATION_MINUTES);
        LocalDateTime pendingSince = pendingCutoff();

        // 사용자별 진행 중 업로드 수 제한 (URL만 대량 발급받아 용량을 선점하는 것 방지)
        if (imageMetaRepository.countActivePendingByUser(userId, pendingSince)
                >= appSettingService.getInt(SettingKey.UPLOAD_MAX_PENDING_PER_USER)) {
            throw new JiburoException(ErrorCode.UPLOAD_PENDING_LIMIT_EXCEEDED);
        }

        // 전체 저장 용량 한도 (관리자 설정). 완료 파일 + 아직 업로드될 수 있는 PENDING만 센다
        long capacityBytes = appSettingService.getInt(SettingKey.UPLOAD_TOTAL_CAPACITY_GB) * GB;
        long currentTotalSize = imageMetaRepository.sumUsedFileSize(pendingSince);
        if (currentTotalSize + request.fileSize() > capacityBytes) {
            throw new JiburoException(ErrorCode.STORAGE_CAPACITY_EXCEEDED);
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

    @Override
    public long usedBytes() {
        return imageMetaRepository.sumUsedFileSize(pendingCutoff());
    }

    /** 업로드되지 않고 방치된 PENDING 정리 (use_yn=false). 업로드된 적이 없으므로 R2에서 지울 객체는 없다 */
    @Scheduled(fixedDelayString = "${app.image.pending-cleanup-interval-ms:600000}",
            initialDelayString = "${app.image.pending-cleanup-initial-delay-ms:600000}")
    @Transactional
    public void cleanupStalePending() {
        int disabled = imageMetaRepository.disableStalePending(pendingCutoff());
        if (disabled > 0) {
            log.info("방치된 업로드(PENDING) {}건 정리", disabled);
        }
    }

    // 이 시각 이전에 발급된 PENDING은 URL이 만료돼 더 이상 업로드될 수 없다
    private LocalDateTime pendingCutoff() {
        long expirationMinutes = appSettingService.getInt(SettingKey.UPLOAD_PRESIGNED_EXPIRATION_MINUTES);
        return LocalDateTime.now().minusMinutes(expirationMinutes).minus(PENDING_GRACE);
    }
}
