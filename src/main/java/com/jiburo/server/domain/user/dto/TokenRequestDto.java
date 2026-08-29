package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenRequestDto(
        @Schema(description = "만료된 Access Token (만료 상태여도 클레임 추출용으로 사용됨)") String accessToken,
        @Schema(description = "Refresh Token") String refreshToken
) {
    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "TokenRequestDto(accessToken=MASKED, refreshToken=MASKED)";
    }
}
