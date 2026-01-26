package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.service.AuthService;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    @AuditLog(action = CodeConst.LogAction.AUTH_REISSUE)
    public ApiResponse<TokenResponseDto> reissue(@RequestBody TokenRequestDto requestDto) {
        TokenResponseDto tokenDto = authService.reissue(requestDto);
        return ApiResponse.success(tokenDto);
    }
}
