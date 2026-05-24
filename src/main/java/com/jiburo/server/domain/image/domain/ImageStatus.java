package com.jiburo.server.domain.image.domain;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageStatus implements CommonCodeType {
    // 임시 URL 발급 후 대기 상태
    PENDING("PENDING", "img.status.pending"),

    // 스토리지 업로드 및 DB 반영 완료 상태
    COMPLETED("COMPLETED", "img.status.completed");

    private final String code;           // DB의 code 컬럼값
    private final String descriptionKey; // 다국어 처리를 위한 키

    @Override
    public String getGroupCode() {
        return "IMG_STATUS";
    }
}
