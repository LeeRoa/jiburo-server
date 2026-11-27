package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.AuditLog;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AdminAuditLogDtos {

    private AdminAuditLogDtos() {
    }

    public record SearchCondition(
            @Schema(description = "사용자 ID") UUID userId,
            @Schema(description = "액션 (정확히 일치)") LogActionType action,
            @Schema(description = "액션 접두사 (예: ADMIN_, AUTH_, POST_) — action이 있으면 무시", example = "ADMIN_") String actionPrefix,
            @Schema(description = "시작일 (포함)", example = "2026-11-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Schema(description = "종료일 (포함)", example = "2026-11-03") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {}

    public record LogResponse(
            @Schema(description = "로그 ID (인코딩된 문자열)") String id,
            @Schema(description = "사용자 ID") UUID userId,
            @Schema(description = "사용자 닉네임 (탈퇴 등으로 없으면 null)") String nickname,
            @Schema(description = "액션") LogActionType action,
            @Schema(description = "대상 데이터 (요청 경로 변수·DTO 요약)") String targetData,
            @Schema(description = "IP") String clientIp,
            @Schema(description = "발생 시각") LocalDateTime createdAt
    ) {
        public static LogResponse of(AuditLog log, String nickname) {
            return new LogResponse(HashidsUtils.encode(log.getId()), log.getUserId(), nickname, log.getAction(),
                    log.getTargetData(), log.getClientIp(), log.getCreatedAt());
        }
    }
}
