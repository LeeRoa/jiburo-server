package com.jiburo.server.domain.post.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalType implements CommonCodeType {
    DOG("DOG", "post.animal.dog"),
    CAT("CAT", "post.animal.cat"),
    ETC("ETC", "post.animal.etc");

    private final String code;
    private final String descriptionKey;

    @Override public String getGroupCode() { return CategoryType.ANIMAL.getCode(); }
}