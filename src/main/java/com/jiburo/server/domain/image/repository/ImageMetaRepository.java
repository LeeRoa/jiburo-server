package com.jiburo.server.domain.image.repository;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageMetaRepository extends JpaRepository<ImageMeta, Long> {

    /**
     * 저장소 사용량 합산.
     * 업로드 완료(COMPLETED) 파일 + 아직 업로드 중일 수 있는 최근 PENDING(pendingSince 이후 발급)만 센다.
     * 오래된 PENDING은 URL이 만료돼 더는 업로드될 수 없으므로 용량을 차지하지 않는다
     * (안 그러면 URL만 반복 발급받아 전체 용량을 채우는 방식으로 모든 사용자의 업로드를 막을 수 있음).
     */
    @Query("SELECT COALESCE(SUM(i.fileSize), 0) FROM ImageMeta i WHERE i.useYn = true "
            + "AND (i.statusCode = com.jiburo.server.domain.image.domain.ImageStatus.COMPLETED OR i.createdAt >= :pendingSince)")
    Long sumUsedFileSize(@Param("pendingSince") LocalDateTime pendingSince);

    // 사용자별 사용량 (위와 같은 기준) — 사용자별 용량 한도용. 관리자 전용 업로드 대상(excluded)은 빼고 센다
    @Query("SELECT COALESCE(SUM(i.fileSize), 0) FROM ImageMeta i WHERE i.user.id = :userId AND i.useYn = true "
            + "AND i.fileCode NOT IN :excluded "
            + "AND (i.statusCode = com.jiburo.server.domain.image.domain.ImageStatus.COMPLETED OR i.createdAt >= :pendingSince)")
    Long sumUsedFileSizeByUser(@Param("userId") UUID userId, @Param("pendingSince") LocalDateTime pendingSince,
                               @Param("excluded") Collection<com.jiburo.server.domain.image.domain.UploadTargetType> excluded);

    // 사용자별 진행 중(최근 발급, 미완료) 업로드 수 — 발급 남용 제한용
    @Query("SELECT COUNT(i) FROM ImageMeta i WHERE i.user.id = :userId AND i.useYn = true "
            + "AND i.statusCode = com.jiburo.server.domain.image.domain.ImageStatus.PENDING AND i.createdAt >= :pendingSince")
    long countActivePendingByUser(@Param("userId") UUID userId, @Param("pendingSince") LocalDateTime pendingSince);

    // 업로드되지 않고 방치된 PENDING을 사용 안 함 처리 (정기 정리)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ImageMeta i SET i.useYn = false WHERE i.useYn = true "
            + "AND i.statusCode = com.jiburo.server.domain.image.domain.ImageStatus.PENDING AND i.createdAt < :before")
    int disableStalePending(@Param("before") LocalDateTime before);

    // 모니터링: 업로드 URL만 받고 완료되지 않은 파일 (오래된 PENDING = 업로드 실패 의심)
    long countByStatusCodeAndCreatedAtBefore(ImageStatus status, LocalDateTime before);

    // R2 저장 경로로 메타데이터 단건 조회
    Optional<ImageMeta> findByFileKeyAndUseYnTrue(String fileKey);

    // 여러 파일 키를 한 번에 조회 (게시글·제보 첨부 검증)
    List<ImageMeta> findAllByFileKeyInAndUseYnTrue(Collection<String> fileKeys);
}
