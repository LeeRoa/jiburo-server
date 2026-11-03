package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtFilter;
import com.jiburo.server.domain.user.service.AuthService;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "사용자(유저) 관련 API")
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/reissue")
    @AuditLog(action = LogActionType.AUTH_REISSUE)
    public ApiResponse<TokenResponseDto> reissue(@Valid @RequestBody TokenRequestDto requestDto) {
        TokenResponseDto tokenDto = authService.reissue(requestDto);
        return ApiResponse.success(tokenDto);
    }

    @Override
    @PostMapping("/login/kakao")
    public ApiResponse<TokenResponseDto> kakaoLogin(@Valid @RequestBody KakaoLoginRequestDto requestDto) {
        TokenResponseDto tokenDto = authService.kakaoLogin(requestDto);
        return ApiResponse.success(tokenDto);
    }

    @Override
    @PostMapping("/logout")
    @AuditLog(action = LogActionType.AUTH_LOGOUT)
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CustomOAuth2User user,
            HttpServletRequest request
    ) {
        // /api/v1/auth/** 는 permitAll 이므로 컨트롤러에서 인증 여부를 확인한다.
        if (user == null) throw new JiburoException(ErrorCode.UNAUTHORIZED);
        authService.logout(user.getUserId(), JwtFilter.resolveToken(request));
        return ApiResponse.success();
    }
}
