package com.jiburo.server.domain.report.domain.enums;

import com.jiburo.server.global.domain.enums.CommonCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportTargetType implements CommonCodeType {
    POST("POST", "report.target.post"),
    CHAT_MESSAGE("CHAT_MESSAGE", "report.target.chat_message"),
    USER("USER", "report.target.user");

    private final String code;
    private final String descriptionKey;

    @Override
    public String getGroupCode() {
        return "REPORT_TARGET";
    }
}
