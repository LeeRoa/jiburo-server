package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.DashboardResponse;
import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.Period;
import com.jiburo.server.domain.admin.service.AdminDashboardService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/dashboard")
@Tag(name = "Admin-Dashboard", description = "[관리자] 대시보드 API")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "[관리자] 대시보드",
            description = "기간별 요약 지표(가입/게시글/해결/채팅방), 일별 추이, 스토리지 사용량, 최근 관리자 작업(감사 로그 권한 보유 시)")
    @GetMapping
    @PreAuthorize("@adminAccess.read('DASHBOARD')")
    public ApiResponse<DashboardResponse> get(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User admin,
            @Parameter(description = "기간 (TODAY / DAYS_7 / DAYS_30)") @RequestParam(defaultValue = "DAYS_7") Period period
    ) {
        return ApiResponse.success(adminDashboardService.get(admin.getUserId(), period));
    }
}
