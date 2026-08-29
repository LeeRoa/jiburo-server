package com.jiburo.server.domain.image.dto;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record PresignedUrlRequestDto(
        @Schema(description = "업로드 대상 폴더 구분 (CHAT/POST/PROFILE 등)") UploadTargetType fileCode,
        @Schema(description = "원본 파일명") String originalFileName,
        @Schema(description = "파일 확장자 (점 제외, 예: png)") String extension,
        @Schema(description = "파일 크기 (byte)") Long fileSize
) {
    public ImageMeta toEntity(User user, String objectKey) {
        return ImageMeta.builder()
                .user(user)
                .fileCode(this.fileCode)
                .fileKey(objectKey)
                .originalFileName(this.originalFileName)
                .extension(this.extension)
                .fileSize(this.fileSize)
                .statusCode(ImageStatus.PENDING)
                .build();
    }
}