package com.jiburo.server.domain.report.dto;

import com.jiburo.server.domain.report.domain.enums.ReportReason;
import com.jiburo.server.domain.report.domain.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequestDto(
        @Schema(description = "신고 대상 종류 (POST / CHAT_MESSAGE / USER)") @NotNull ReportTargetType targetType,
        @Schema(description = "대상 ID — POST·CHAT_MESSAGE는 인코딩된 문자열, USER는 사용자 UUID") @NotBlank String targetId,
        @Schema(description = "신고 사유 (공통코드 REPORT_REASON)") @NotNull ReportReason reason,
        @Schema(description = "상세 내용 (사유가 ETC면 필수, 최대 500자)") @Size(max = 500) String detail
) {}
