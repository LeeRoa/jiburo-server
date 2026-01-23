package com.jiburo.server.domain.user.dto;

import lombok.Builder;

@Builder
public record TokenResponseDto(
        String grantType,   // Bearer
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn
) {}