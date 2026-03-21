package com.jiburo.server.global.dto;

public record PresignedUrlResponseDto(
        String presignedUrl,
        String fileName
) {
}