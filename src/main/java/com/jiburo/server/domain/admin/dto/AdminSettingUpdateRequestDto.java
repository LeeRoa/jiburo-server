package com.jiburo.server.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminSettingUpdateRequestDto(
        @Schema(description = "새 값 (문자열). 타입과 허용 범위는 설정 목록 응답의 type/min/max 참고. STRING_LIST는 콤마 구분",
                example = "8")
        @NotNull @Size(max = 1000)
        String value
) {}
