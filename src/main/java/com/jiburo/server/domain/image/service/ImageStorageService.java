package com.jiburo.server.domain.image.service;

import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;

import java.util.UUID;

public interface ImageStorageService {
    PresignedUrlResponseDto createPresignedUrl(UUID userId, PresignedUrlRequestDto request);
}
