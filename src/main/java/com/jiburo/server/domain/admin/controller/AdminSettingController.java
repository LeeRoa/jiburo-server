package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminSettingResponseDto;
import com.jiburo.server.domain.admin.dto.AdminSettingUpdateRequestDto;
import com.jiburo.server.domain.admin.service.AdminSettingService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/settings")
@Tag(name = "Admin-Setting", description = "[관리자] 운영 설정 관리 API")
public class AdminSettingController implements AdminSettingControllerDocs {

    private final AdminSettingService adminSettingService;

    @Override
    @GetMapping
    public ApiResponse<List<AdminSettingResponseDto>> getSettings() {
        return ApiResponse.success(adminSettingService.findAll());
    }

    @Override
    @GetMapping("/{key}")
    public ApiResponse<AdminSettingResponseDto> getSetting(@PathVariable String key) {
        return ApiResponse.success(adminSettingService.find(key));
    }

    @Override
    @PutMapping("/{key}")
    @AuditLog(action = LogActionType.ADMIN_SETTING_UPDATE)
    public ApiResponse<AdminSettingResponseDto> updateSetting(
            @AuthenticationPrincipal CustomOAuth2User admin,
            @PathVariable String key,
            @Valid @RequestBody AdminSettingUpdateRequestDto requestDto
    ) {
        return ApiResponse.success(adminSettingService.update(key, requestDto.value(), admin.getUserId()));
    }

    @Override
    @DeleteMapping("/{key}")
    @AuditLog(action = LogActionType.ADMIN_SETTING_RESET)
    public ApiResponse<AdminSettingResponseDto> resetSetting(@PathVariable String key) {
        return ApiResponse.success(adminSettingService.reset(key));
    }
}
