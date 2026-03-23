package com.jiburo.server.domain.image.dto;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.CodeConst;

public record PresignedUrlRequestDto(
        String fileCode,
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
                .statusCode(CodeConst.ImgStatus.PENDING)
                .build();
    }
}