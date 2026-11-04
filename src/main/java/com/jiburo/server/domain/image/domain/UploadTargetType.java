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
    PROFILE("PROFILE", "upload.target.profile", "profile/images", Set.of("jpg", "jpeg", "png", "webp")),
    FINDER_IMAGE("FINDER_IMAGE", "upload.target.finder.image", "finder/images", Set.of("jpg", "jpeg", "png", "webp")),
    FINDER_VIDEO("FINDER_VIDEO", "upload.target.finder.video", "finder/videos", Set.of("mp4", "mov", "webm")),
    // 관리자 전용 (일반 업로드 API로는 발급 불가). gif/webp는 움직이는 이모티콘 허용
    EMOTICON("EMOTICON", "upload.target.emoticon", "emoticon/images", Set.of("png", "webp", "gif"));

    private final String code;           // DB의 code 컬럼에 저장될 값 (예: CHAT)
    private final String descriptionKey; // 프론트엔드 다국어 처리용 키
    private final String folderPath;     // 실제 스토리지(R2/Local)에 저장될 경로
    private final Set<String> allowedExtensions; // 업로드 허용 확장자 (소문자)

    public boolean isExtensionAllowed(String extension) {
        return extension != null && allowedExtensions.contains(extension.toLowerCase());
    }

    // 동영상 업로드 대상 여부 (파일 1개 최대 크기 설정을 이미지/동영상으로 구분하는 데 사용)
    public boolean isVideo() {
        return this == CHAT_VIDEO || this == FINDER_VIDEO;
    }

    // 관리자 API에서만 업로드 URL을 발급하는 대상
    public boolean isAdminOnly() {
        return this == EMOTICON;
    }

    @Override
    public String getGroupCode() { return "UPLOAD_TARGET"; }

    @Override
    public String getValue() { return this.folderPath; }

    @Override
    public String getRef2() { return String.join(",", this.allowedExtensions); }
}
