package com.jiburo.server.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequestDto(
        @NotBlank
        String accessToken   // 안드로이드 Kakao SDK가 발급한 카카오 액세스 토큰
) {}
