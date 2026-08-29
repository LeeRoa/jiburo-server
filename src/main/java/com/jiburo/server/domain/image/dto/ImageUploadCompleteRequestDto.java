package com.jiburo.server.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadCompleteRequestDto(
        @Schema(description = "presigned URL 발급 시 받은 파일 키 (R2 objectKey)") String fileKey
) {}