package com.jiburo.server.domain.report.controller;

import com.jiburo.server.domain.report.dto.ReportCreateRequestDto;
import com.jiburo.server.domain.report.service.ReportService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@Tag(name = "Report", description = "신고 API")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고하기",
            description = "게시글·채팅 메시지·사용자를 신고합니다. 사유 선택지는 공통코드 REPORT_REASON. "
                    + "자기 자신/자기 콘텐츠는 신고 불가(CANNOT_REPORT_SELF), 처리 전 같은 대상 재신고는 ALREADY_REPORTED. "
                    + "응답은 신고 ID이며, 처리되면 알림(REPORT_HANDLED)이 갑니다.")
    @PostMapping
    @AuditLog(action = LogActionType.REPORT_CREATE)
    public ApiResponse<String> create(@Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user,
                                      @Valid @RequestBody ReportCreateRequestDto request) {
        return ApiResponse.success(reportService.create(user.getUserId(), request));
    }
}
