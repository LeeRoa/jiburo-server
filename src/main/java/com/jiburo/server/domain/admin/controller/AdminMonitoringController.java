package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.MonitoringResponse;
import com.jiburo.server.domain.admin.service.AdminMonitoringService;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/monitoring")
@Tag(name = "Admin-Monitoring", description = "[관리자] 시스템 모니터링 API")
public class AdminMonitoringController {

    private final AdminMonitoringService adminMonitoringService;

    @Operation(summary = "[관리자] 시스템 상태",
            description = "종합 상태(UP/WARN/DOWN + 사유), 서버·JVM, DB·Redis 응답 시간, 업로드 저장소, "
                    + "채팅 접속자·처리 대기 신고·미완료 업로드, 최근 500 오류. 요청을 받은 서버 기준 값입니다.")
    @GetMapping
    @PreAuthorize("@adminAccess.read('MONITORING')")
    public ApiResponse<MonitoringResponse> get() {
        return ApiResponse.success(adminMonitoringService.get());
    }
}
