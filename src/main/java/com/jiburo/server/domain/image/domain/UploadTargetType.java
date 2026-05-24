package com.jiburo.server.domain.image.domain;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadTargetType implements CommonCodeType {
    CHAT("CHAT", "upload.target.chat", "chat/images"),
    POST("POST", "upload.target.post", "post/images"),
    PROFILE("PROFILE", "upload.target.profile", "profile/images");

    private final String code;           // DB의 code 컬럼에 저장될 값 (예: CHAT)
    private final String descriptionKey; // 프론트엔드 다국어 처리용 키
    private final String folderPath;     // 실제 스토리지(R2/Local)에 저장될 경로

    @Override
    public String getGroupCode() {
        return "UPLOAD_TARGET";
    }
}
