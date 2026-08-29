package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.service.AuthService;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "사용자(유저) 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    @AuditLog(action = LogActionType.AUTH_REISSUE)
    public ApiResponse<TokenResponseDto> reissue(@RequestBody TokenRequestDto requestDto) {
        TokenResponseDto tokenDto = authService.reissue(requestDto);
        return ApiResponse.success(tokenDto);
    }

    // 안드로이드 앱이 Kakao SDK로 직접 로그인 후 발급받은 accessToken을 검증해 자체 JWT를 발급
    @PostMapping("/login/kakao")
    public ApiResponse<TokenResponseDto> kakaoLogin(@Valid @RequestBody KakaoLoginRequestDto requestDto) {
        TokenResponseDto tokenDto = authService.kakaoLogin(requestDto);
        return ApiResponse.success(tokenDto);
    }
}
