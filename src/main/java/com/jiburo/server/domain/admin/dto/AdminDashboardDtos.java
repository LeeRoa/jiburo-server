package com.jiburo.server.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public final class AdminDashboardDtos {

    private AdminDashboardDtos() {
    }

    public enum Period {
        TODAY(1), DAYS_7(7), DAYS_30(30);

        private final int days;

        Period(int days) {
            this.days = days;
        }

        public int days() {
            return days;
        }
    }

    public record Summary(
            @Schema(description = "신규 가입자 수") long newUsers,
            @Schema(description = "신규 게시글 수 (이후 삭제된 글 포함)") long newPosts,
            @Schema(description = "해결 완료 게시글 수 (COMPLETE 상태 + 마지막 수정 시각 기준 근사)") long completedPosts,
            @Schema(description = "신규 채팅방 수") long newChatRooms
    ) {}

    public record Storage(
            @Schema(description = "사용 중인 용량 (byte)") long usedBytes,
            @Schema(description = "한도 (byte, 운영 설정 UPLOAD_TOTAL_CAPACITY_GB)") long capacityBytes,
            @Schema(description = "사용률 (0~100, 소수 1자리)") double usagePercent,
            @Schema(description = "경고 여부 (사용률 80% 이상)") boolean warning
    ) {}

    public record DailyPoint(
            @Schema(description = "날짜") LocalDate date,
            @Schema(description = "신규 가입자") long newUsers,
            @Schema(description = "신규 게시글") long newPosts
    ) {}

    public record DashboardResponse(
            @Schema(description = "조회 기간") Period period,
            @Schema(description = "시작일 (포함)") LocalDate from,
            @Schema(description = "종료일 (포함)") LocalDate to,
            Summary summary,
            Storage storage,
            @Schema(description = "일별 추이 (값이 0인 날도 포함)") List<DailyPoint> daily,
            @Schema(description = "최근 관리자 작업 5건 — 감사 로그 메뉴 노출 권한이 없으면 null")
            List<AdminAuditLogDtos.LogResponse> recentAdminActions
    ) {}
}
