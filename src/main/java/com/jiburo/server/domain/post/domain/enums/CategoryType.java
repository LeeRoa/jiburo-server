package com.jiburo.server.domain.post.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryType implements CommonCodeType {
    ANIMAL("ANIMAL", "category.animal"),
    PERSON("PERSON", "category.person"),
    ITEM("ITEM", "category.item"),
    ETC("ETC", "category.etc");

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() { return "CATEGORY"; }
}