package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto(
        @Deprecated
        @Schema(description = "(사용 안 함) 이전 버전 호환용 필드. 서버는 Refresh Token만으로 사용자를 식별합니다.", deprecated = true)
        String accessToken,

        @Schema(description = "Refresh Token")
        @NotBlank
        String refreshToken
) {
    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "TokenRequestDto(accessToken=MASKED, refreshToken=MASKED)";
    }
}
