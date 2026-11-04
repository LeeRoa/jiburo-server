package com.jiburo.server.domain.report.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus implements CommonCodeType {
    PENDING("PENDING", "report.status.pending"),     // 접수 (처리 대기)
    RESOLVED("RESOLVED", "report.status.resolved"),  // 조치 완료 (콘텐츠 삭제 / 정지)
    DISMISSED("DISMISSED", "report.status.dismissed"); // 기각 (조치 없음)

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() {
        return "REPORT_STATUS";
    }
}
