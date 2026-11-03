package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;

import java.util.UUID;

public interface AuthService {

    TokenResponseDto reissue(TokenRequestDto requestDto);

    TokenResponseDto kakaoLogin(KakaoLoginRequestDto requestDto);

    void logout(UUID userId, String accessToken);
}
