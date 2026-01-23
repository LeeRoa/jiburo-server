package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.service.AuthService;
import com.jiburo.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/reissue")
    public ApiResponse<TokenResponseDto> reissue(@RequestBody TokenRequestDto requestDto) {
        TokenResponseDto tokenDto = authService.reissue(requestDto);
        return ApiResponse.success(tokenDto);
    }
}
