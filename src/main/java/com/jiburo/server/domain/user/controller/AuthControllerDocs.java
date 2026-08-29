package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;

public interface AuthControllerDocs {

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새 Access/Refresh Token 쌍을 발급받습니다.")
    ApiResponse<TokenResponseDto> reissue(TokenRequestDto requestDto);

    @Operation(summary = "카카오 앱 로그인", description = "안드로이드/iOS 앱이 Kakao SDK로 로그인해 발급받은 accessToken을 검증하고 자체 JWT를 발급합니다.")
    ApiResponse<TokenResponseDto> kakaoLogin(KakaoLoginRequestDto requestDto);
}
