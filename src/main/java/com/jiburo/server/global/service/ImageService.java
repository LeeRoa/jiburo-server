package com.jiburo.server.global.service;

import com.jiburo.server.global.dto.PresignedUrlResponseDto;

public interface ImageService {
    PresignedUrlResponseDto createPresignedUrl(String extension);
}
