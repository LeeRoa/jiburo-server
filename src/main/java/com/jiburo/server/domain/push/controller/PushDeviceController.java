package com.jiburo.server.domain.push.controller;

import com.jiburo.server.domain.push.dto.PushDeviceRequestDto;
import com.jiburo.server.domain.push.service.PushService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/push/devices")
@Tag(name = "Push", description = "푸시 알림 기기 등록 API")
public class PushDeviceController {

    private final PushService pushService;

    @Operation(summary = "푸시 기기 등록",
            description = "로그인 직후와 FCM 토큰이 바뀔 때(onTokenRefresh) 호출. 푸시 문구 언어는 요청의 Accept-Language. "
                    + "같은 기기에 다른 계정으로 로그인하면 그 계정으로 옮겨집니다. 관리자 계정은 운영자 알림도 이 기기로 받습니다.")
    @PostMapping
    public ApiResponse<Void> register(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user,
                                      @Valid @RequestBody PushDeviceRequestDto request) {
        if (request.platform() == null) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        pushService.register(user.getUserId(), request.token(), request.platform(), LocaleContextHolder.getLocale());
        return ApiResponse.success();
    }

    @Operation(summary = "푸시 기기 해제", description = "로그아웃 직전에 호출. 이 기기로 더 이상 이 계정의 푸시를 받지 않습니다.")
    @PostMapping("/unregister")
    public ApiResponse<Void> unregister(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user,
                                        @Valid @RequestBody PushDeviceRequestDto request) {
        pushService.unregister(user.getUserId(), request.token());
        return ApiResponse.success();
    }
}
