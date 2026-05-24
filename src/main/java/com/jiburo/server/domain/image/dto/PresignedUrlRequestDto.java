package com.jiburo.server.domain.image.dto;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.user.domain.User;

public record PresignedUrlRequestDto(
        UploadTargetType fileCode,
        String originalFileName,
        String extension,
        Long fileSize
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