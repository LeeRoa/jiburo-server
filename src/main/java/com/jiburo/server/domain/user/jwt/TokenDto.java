package com.jiburo.server.domain.user.jwt;

import lombok.Builder;

@Builder
public record TokenDto(
        String grantType,   // Bearer
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {}