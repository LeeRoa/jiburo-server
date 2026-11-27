package com.jiburo.server.domain.image.service;

import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;

import java.util.UUID;

public interface ImageStorageService {
    PresignedUrlResponseDto createPresignedUrl(UUID userId, PresignedUrlRequestDto request);

    void completeUpload(UUID userId, String fileKey);

    /** 저장소 사용량(byte) — 완료 파일 + 아직 업로드될 수 있는 PENDING */
    long usedBytes();
}
