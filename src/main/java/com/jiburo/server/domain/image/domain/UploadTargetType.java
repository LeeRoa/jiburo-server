package com.jiburo.server.domain.image.domain;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum UploadTargetType implements CommonCodeType {
    CHAT("CHAT", "upload.target.chat", "chat/images", Set.of("jpg", "jpeg", "png", "webp", "gif")),
    CHAT_VIDEO("CHAT_VIDEO", "upload.target.chat.video", "chat/videos", Set.of("mp4", "mov", "webm")),
    POST("POST", "upload.target.post", "post/images", Set.of("jpg", "jpeg", "png", "webp", "gif")),
    PROFILE("PROFILE", "upload.target.profile", "profile/images", Set.of("jpg", "jpeg", "png", "webp"));

    private final String code;           // DB의 code 컬럼에 저장될 값 (예: CHAT)
    private final String descriptionKey; // 프론트엔드 다국어 처리용 키
    private final String folderPath;     // 실제 스토리지(R2/Local)에 저장될 경로
    private final Set<String> allowedExtensions; // 업로드 허용 확장자 (소문자)

    public boolean isExtensionAllowed(String extension) {
        return extension != null && allowedExtensions.contains(extension.toLowerCase());
    }

    @Override
    public String getGroupCode() {
        return "UPLOAD_TARGET";
    }
}
