package com.jiburo.server.domain.report.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 신고 사유 (앱은 공통코드 REPORT_REASON + label로 선택지를 그린다) */
@Getter
@RequiredArgsConstructor
public enum ReportReason implements CommonCodeType {
    SPAM("SPAM", "report.reason.spam"),                     // 광고·도배
    ABUSE("ABUSE", "report.reason.abuse"),                  // 욕설·비방·괴롭힘
    FALSE_INFO("FALSE_INFO", "report.reason.false_info"),   // 허위 분실/제보
    INAPPROPRIATE("INAPPROPRIATE", "report.reason.inappropriate"), // 음란·불쾌한 콘텐츠
    FRAUD("FRAUD", "report.reason.fraud"),                  // 사기·금전 요구
    ETC("ETC", "report.reason.etc");                        // 기타 (상세 내용 필수)

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() {
        return "REPORT_REASON";
    }
}
