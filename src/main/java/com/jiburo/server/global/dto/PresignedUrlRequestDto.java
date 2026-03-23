package com.jiburo.server.global.dto;

public record PresignedUrlRequestDto(
        String fileCode,
        String extension,
        Long fileSize
) {}