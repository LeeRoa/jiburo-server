package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequestDto(
        @Schema(description = "안드로이드/iOS Kakao SDK가 발급한 카카오 액세스 토큰")
        @NotBlank
        String accessToken
) {}
