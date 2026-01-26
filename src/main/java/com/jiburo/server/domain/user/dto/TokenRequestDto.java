package com.jiburo.server.domain.user.dto;

public record TokenRequestDto(
        String accessToken,
        String refreshToken
) {
    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "TokenRequestDto(accessToken=MASKED, refreshToken=MASKED)";
    }
}
