package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminMeResponseDto;
import com.jiburo.server.domain.admin.service.AdminAuthorizationService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/me")
@Tag(name = "Admin-Me", description = "[관리자] 내 관리자 정보")
public class AdminMeController {

    private final AdminAuthorizationService adminAuthorizationService;

    @Operation(summary = "[관리자] 내 역할과 노출 메뉴",
            description = "관리자 웹 진입 직후 호출해 좌측 메뉴와 편집 버튼 노출 여부를 구성합니다. 관리자 역할이 없으면 403.")
    @GetMapping
    @PreAuthorize("@adminAccess.any()")
    public ApiResponse<AdminMeResponseDto> me(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user) {
        return ApiResponse.success(AdminMeResponseDto.of(user.getUserId(), adminAuthorizationService.getAccess(user.getUserId())));
    }
}
