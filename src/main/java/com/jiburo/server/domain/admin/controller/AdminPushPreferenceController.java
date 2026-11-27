package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.service.AdminAlertService;
import com.jiburo.server.domain.admin.service.AdminAlertService.PreferenceResponse;
import com.jiburo.server.domain.push.service.PushSender;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/me/push-preferences")
@Tag(name = "Admin-Push", description = "[관리자] 내 운영자 푸시 설정 API")
public class AdminPushPreferenceController {

    private final AdminAlertService adminAlertService;
    private final PushSender pushSender;

    public record UpdateRequest(
            @Schema(description = "시스템 장애·복구 알림 (null이면 유지)") Boolean systemAlert,
            @Schema(description = "저장소 용량 경고 (null이면 유지)") Boolean storageAlert
    ) {}

    @Operation(summary = "[관리자] 내 푸시 설정",
            description = "항목별 수신 여부(기본 켜짐), 내 등록 기기 수, 서버 푸시 활성 여부(FCM 키 설정), "
                    + "실제 수신 대상인지(시스템 모니터링 노출 권한 보유). 기기는 앱에서 POST /api/v1/push/devices로 등록합니다.")
    @GetMapping
    @PreAuthorize("@adminAccess.any()")
    public ApiResponse<PreferenceResponse> get(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User admin) {
        return ApiResponse.success(adminAlertService.preference(admin.getUserId(), pushSender.enabled()));
    }

    @Operation(summary = "[관리자] 내 푸시 설정 변경")
    @PutMapping
    @PreAuthorize("@adminAccess.any()")
    public ApiResponse<PreferenceResponse> update(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User admin,
                                                  @RequestBody UpdateRequest request) {
        adminAlertService.updatePreference(admin.getUserId(), request.systemAlert(), request.storageAlert());
        return ApiResponse.success(adminAlertService.preference(admin.getUserId(), pushSender.enabled()));
    }

    @Operation(summary = "[관리자] 테스트 푸시", description = "내 등록 기기로만 테스트 알림을 보냅니다 (기기 등록 확인용).")
    @PostMapping("/test")
    @PreAuthorize("@adminAccess.any()")
    public ApiResponse<Void> test(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User admin) {
        adminAlertService.sendTest(admin.getUserId());
        return ApiResponse.success();
    }
}
