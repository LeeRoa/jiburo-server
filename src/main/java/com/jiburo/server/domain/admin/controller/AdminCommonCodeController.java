package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.global.cache.CommonCodeCache;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 - 공통코드 관리
 * /api/v1/admin/** 는 SecurityConfig에서 ROLE_ADMIN만 접근 가능하도록 경로 단위로 보호된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/common-codes")
@Tag(name = "Admin-CommonCode", description = "[관리자] 공통코드 관리 API")
public class AdminCommonCodeController implements AdminCommonCodeControllerDocs {

    private final CommonCodeCache commonCodeCache;

    @Override
    @PostMapping("/reload")
    @PreAuthorize("@adminAccess.write('COMMON_CODE')")
    @AuditLog(action = LogActionType.ADMIN_COMMON_CODE_RELOAD)
    public ApiResponse<Void> reload() {
        commonCodeCache.reload();
        return ApiResponse.success();
    }
}
