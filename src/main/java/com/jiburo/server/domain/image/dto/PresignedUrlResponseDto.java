package com.jiburo.server.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PresignedUrlResponseDto(
        @Schema(description = "클라이언트가 PUT 요청을 보낼 R2 presigned URL") String presignedUrl,
        @Schema(description = "업로드 완료 콜백 시 다시 보내야 하는 파일 키 (R2 objectKey)") String fileKey,
        @Schema(description = "PUT 요청에 그대로 보내야 하는 Content-Type 헤더 값 (서명에 포함되어 다르면 R2가 403으로 거절)",
                example = "image/jpeg") String contentType
) {
}
