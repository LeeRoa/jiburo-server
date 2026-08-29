package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record TokenResponseDto(
        @Schema(description = "토큰 타입", example = "Bearer") String grantType,
        @Schema(description = "Access Token") String accessToken,
        @Schema(description = "Refresh Token") String refreshToken,
        @Schema(description = "Access Token 만료 시각 (epoch millis)") Long accessTokenExpiresIn
) {}