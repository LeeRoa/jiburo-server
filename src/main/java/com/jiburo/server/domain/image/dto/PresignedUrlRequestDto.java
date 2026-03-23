package com.jiburo.server.domain.image.dto;

public record PresignedUrlRequestDto(
        String fileCode,
        String extension,
        Long fileSize
) {}