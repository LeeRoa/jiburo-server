package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthControllerDocs {

    @Operation(summary = "토큰 재발급",
            description = "Refresh Token으로 새 Access/Refresh Token 쌍을 발급받습니다. "
                    + "사용한 Refresh Token은 즉시 폐기되므로(Rotation) 응답의 새 Refresh Token으로 교체해 저장해야 합니다.")
    ApiResponse<TokenResponseDto> reissue(TokenRequestDto requestDto);

    @Operation(summary = "카카오 앱 로그인", description = "안드로이드/iOS 앱이 Kakao SDK로 로그인해 발급받은 accessToken을 검증하고 자체 JWT를 발급합니다.")
    ApiResponse<TokenResponseDto> kakaoLogin(KakaoLoginRequestDto requestDto);

    @Operation(summary = "로그아웃",
            description = "Refresh Token을 폐기하고, 요청에 사용한 Access Token을 만료 시각까지 차단합니다. Authorization 헤더 필수.")
    ApiResponse<Void> logout(@Parameter(hidden = true) CustomOAuth2User user,
                             @Parameter(hidden = true) HttpServletRequest request);
}
