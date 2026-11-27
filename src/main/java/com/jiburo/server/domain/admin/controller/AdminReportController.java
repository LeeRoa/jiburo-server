package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminReportDtos.*;
import com.jiburo.server.domain.admin.service.AdminReportService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/reports")
@Tag(name = "Admin-Report", description = "[관리자] 신고 처리 API")
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "[관리자] 신고 목록", description = "기본 최신순. 각 행에 같은 대상의 처리 대기 신고 수 포함")
    @GetMapping
    @PreAuthorize("@adminAccess.read('REPORT')")
    public ApiResponse<Page<ReportSummary>> search(
            @ParameterObject @ModelAttribute SearchCondition condition,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(adminReportService.search(condition, pageable));
    }

    @Operation(summary = "[관리자] 신고 상세",
            description = "신고 당시 내용, 원본 상태, 채팅 신고면 앞뒤 대화(5개씩), 신고당한 사용자의 누적 신고 수·정지 상태, 처리 결과")
    @GetMapping("/{reportId}")
    @PreAuthorize("@adminAccess.read('REPORT')")
    public ApiResponse<ReportDetail> find(@PathVariable String reportId) {
        return ApiResponse.success(adminReportService.find(HashidsUtils.decode(reportId)));
    }

    @Operation(summary = "[관리자] 신고 처리",
            description = "콘텐츠 삭제 / 사용자 정지(기간·영구)를 고르고, 아무 조치도 없으면 기각(DISMISSED). "
                    + "같은 대상의 처리 대기 신고는 모두 같은 결과로 처리되고 신고자에게 알림이 갑니다. "
                    + "관리자 계정은 정지할 수 없습니다(ACCESS_DENIED).")
    @PostMapping("/{reportId}/resolve")
    @PreAuthorize("@adminAccess.write('REPORT')")
    @AuditLog(action = LogActionType.ADMIN_REPORT_RESOLVE)
    public ApiResponse<ResolveResult> resolve(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User admin,
                                              @PathVariable String reportId,
                                              @Valid @RequestBody ResolveRequest request) {
        return ApiResponse.success(adminReportService.resolve(admin.getUserId(), HashidsUtils.decode(reportId), request));
    }
}
