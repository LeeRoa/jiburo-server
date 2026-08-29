package com.jiburo.server.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jiburo.server.global.domain.CommonCode;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonCodeResponseDto(
        @Schema(description = "공통코드 값 (그룹 내 고유 코드)")
        String code,
        @Schema(description = "i18n 메시지 번들 키 (프론트가 다국어 라벨 조립 시 사용)")
        String messageKey,
        @Schema(description = "그룹별로 의미가 다른 자유 참조값 1 (예: 순서, 임계값 등)")
        String ref1,
        @Schema(description = "그룹별로 의미가 다른 자유 참조값 2")
        String ref2
) {
    public static CommonCodeResponseDto from(CommonCode entity) {
        return new CommonCodeResponseDto(
                entity.getCode(),
                entity.getMessageKey(),
                entity.getRef1(),
                entity.getRef2()
        );
    }
}
