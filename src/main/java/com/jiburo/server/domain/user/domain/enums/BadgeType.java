package com.jiburo.server.domain.user.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeType implements CommonCodeType {

    BEGINNER("BEGINNER", "badge.beginner.title", "0"),
    JUNIOR("JUNIOR", "badge.junior.title", "30"),
    SENIOR("SENIOR", "badge.senior.title", "100"),
    MASTER("MASTER", "badge.master.title", "300");

    private final String code;
    private final String descriptionKey;
    private final String requirePoints;

    @Override
    public String getGroupCode() { return "BADGE"; }

    @Override
    public String getValue() { return this.requirePoints; }
}
