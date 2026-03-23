package com.jiburo.server.global.service;

import com.jiburo.server.global.dto.PresignedUrlRequestDto;
import com.jiburo.server.global.dto.PresignedUrlResponseDto;

public interface R2Service {
    PresignedUrlResponseDto createPresignedUrl(PresignedUrlRequestDto request);
}
