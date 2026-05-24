package com.jiburo.server.domain.post.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisibilityType implements CommonCodeType {

    // 1. 전체 공개 (누구나 볼 수 있음)
    PUBLIC("PUBLIC", "post.visibility.public"),

    // 2. 보호된 공개 (특정 권한이나 허용된 대상만 볼 수 있음)
    PROTECTED("PROTECTED", "post.visibility.protected"),

    // 3. 비공개 (작성자 본인만 볼 수 있음)
    PRIVATE("PRIVATE", "post.visibility.private");

    private final String code;           // DB의 code 컬럼값 (예: PUBLIC)
    private final String descriptionKey; // 프론트엔드 다국어 키 (예: post.visibility.public)

    @Override
    public String getGroupCode() {return "VISIBILITY";}

    /**
     * 비즈니스 로직: 해당 글이 공개 상태인지 확인
     */
    public boolean isPublic() {
        return this == PUBLIC;
    }

    /**
     * 비즈니스 로직: 해당 글이 본인만 볼 수 있는 상태인지 확인
     */
    public boolean isPrivate() {
        return this == PRIVATE;
    }
}