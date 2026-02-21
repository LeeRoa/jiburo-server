package com.jiburo.server.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jiburo.server.global.domain.CommonCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonCodeResponseDto(
        String code,
        String ref1,
        String ref2
) {
    public static CommonCodeResponseDto from(CommonCode entity) {
        return new CommonCodeResponseDto(
                entity.getCode(),
                entity.getRef1(),
                entity.getRef2()
        );
    }
}
