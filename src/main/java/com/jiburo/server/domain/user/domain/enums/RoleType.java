package com.jiburo.server.domain.user.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType implements CommonCodeType {
    USER("USER", "role.user"),
    ADMIN("ADMIN", "role.admin");

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() { return "ROLE"; }
}