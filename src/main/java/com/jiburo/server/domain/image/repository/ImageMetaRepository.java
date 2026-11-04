package com.jiburo.server.domain.image.repository;

import com.jiburo.server.domain.image.domain.ImageMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ImageMetaRepository extends JpaRepository<ImageMeta, Long> {
    // 이전에 만든 용량 합산 쿼리
    @Query("SELECT COALESCE(SUM(i.fileSize), 0) FROM ImageMeta i WHERE i.useYn = true")
    Long sumTotalFileSize();

    // 모니터링: 업로드 URL만 받고 완료되지 않은 파일 (오래된 PENDING = 업로드 실패 의심)
    long countByStatusCodeAndCreatedAtBefore(com.jiburo.server.domain.image.domain.ImageStatus status, java.time.LocalDateTime before);

    // R2 저장 경로로 메타데이터 단건 조회
    Optional<ImageMeta> findByFileKeyAndUseYnTrue(String fileKey);
}