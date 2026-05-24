package com.jiburo.server.domain.post.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostStatus implements CommonCodeType {
    LOST("LOST", "post.status.lost"),
    PROTECTING("PROTECTING", "post.status.protecting"),
    COMPLETE("COMPLETE", "post.status.complete");

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() { return "STATUS"; }
}