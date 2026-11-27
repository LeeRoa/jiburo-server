package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginCodeExchangeRequestDto(
        @Schema(description = "웹 OAuth 로그인 후 redirect URL의 code 쿼리 파라미터 값 (1회용, 1분 내 사용)")
        @NotBlank
        String code
) {
    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "LoginCodeExchangeRequestDto(code=MASKED)";
    }
}
