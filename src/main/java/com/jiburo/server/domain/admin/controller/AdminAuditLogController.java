package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos.LogResponse;
import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos.SearchCondition;
import com.jiburo.server.domain.admin.service.AdminAuditLogService;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/audit-logs")
@Tag(name = "Admin-AuditLog", description = "[관리자] 감사 로그 조회 API")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "[관리자] 감사 로그 검색",
            description = "사용자, 액션(정확히 또는 접두사), 기간으로 검색합니다. 최신순. 예: actionPrefix=ADMIN_ → 관리자 작업만")
    @GetMapping
    @PreAuthorize("@adminAccess.read('AUDIT_LOG')")
    public ApiResponse<Page<LogResponse>> search(
            @ParameterObject @ModelAttribute SearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(adminAuditLogService.search(condition, pageable));
    }
}
