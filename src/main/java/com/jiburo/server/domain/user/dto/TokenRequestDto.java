package com.jiburo.server.domain.user.dto;

public record TokenRequestDto(
        String accessToken,
        String refreshToken
) {}
