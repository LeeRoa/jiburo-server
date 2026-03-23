package com.jiburo.server.domain.image.dto;

public record PresignedUrlResponseDto(
        String presignedUrl,
        String fileKey
) {
}