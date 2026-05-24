package com.jiburo.server.domain.post.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GenderType implements CommonCodeType {
    MALE("MALE", "post.gender.male"),
    FEMALE("FEMALE", "post.gender.female"),
    UNKNOWN("UNKNOWN", "post.gender.unknown");

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() { return "GENDER"; }
}
