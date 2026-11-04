package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.global.monitoring.ServerErrorRecorder.ErrorEntry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminMonitoringDtos {

    private AdminMonitoringDtos() {
    }

    public enum Level {
        @Schema(description = "정상") UP,
        @Schema(description = "주의 (서비스는 동작)") WARN,
        @Schema(description = "장애 (DB·Redis 연결 실패 등)") DOWN
    }

    public record Overall(
            @Schema(description = "종합 상태") Level level,
            @Schema(description = "주의·장애 사유 (사람이 읽는 문장)") List<String> reasons
    ) {}

    public record ServerInfo(
            @Schema(description = "이 응답을 만든 서버 이름 (여러 대면 서버마다 값이 다름)") String instance,
            @Schema(description = "기동 시각") LocalDateTime startedAt,
            @Schema(description = "가동 시간 (초)") long uptimeSeconds,
            @Schema(description = "Java 버전") String javaVersion,
            @Schema(description = "활성 프로파일") List<String> profiles
    ) {}

    public record Jvm(
            @Schema(description = "힙 사용량 (byte)") long heapUsedBytes,
            @Schema(description = "힙 최대 (byte)") long heapMaxBytes,
            @Schema(description = "힙 사용률 (0~100)") double heapUsagePercent,
            @Schema(description = "스레드 수") int threadCount,
            @Schema(description = "CPU 코어 수") int processors,
            @Schema(description = "프로세스 CPU 사용률 (0~100, 측정 불가면 -1)") double processCpuPercent,
            @Schema(description = "시스템 CPU 사용률 (0~100, 측정 불가면 -1)") double systemCpuPercent
    ) {}

    public record Dependency(
            @Schema(description = "이름 (database / redis)") String name,
            @Schema(description = "상태") Level level,
            @Schema(description = "응답 시간 (ms, 실패면 -1)") long latencyMs,
            @Schema(description = "실패 사유") String error
    ) {}

    public record Activity(
            @Schema(description = "실시간 채팅(STOMP) 접속 사용자 수 — 이 서버 기준") int chatConnectedUsers,
            @Schema(description = "처리 대기 신고 수") long pendingReports,
            @Schema(description = "1시간 넘게 완료되지 않은 업로드 수 (업로드 실패 의심)") long staleUploads
    ) {}

    public record Errors(
            @Schema(description = "기동 후 500 오류 총 건수 — 이 서버 기준") long totalSinceStart,
            @Schema(description = "최근 1시간 500 오류 건수 (최근 50건 안에서 집계)") long lastHour,
            @Schema(description = "최근 500 오류 (최신순, 최대 50건)") List<ErrorEntry> recent
    ) {}

    public record MonitoringResponse(
            @Schema(description = "조회 시각") LocalDateTime checkedAt,
            Overall overall,
            ServerInfo server,
            Jvm jvm,
            List<Dependency> dependencies,
            @Schema(description = "업로드 저장소 사용량 (대시보드와 같은 값)") AdminDashboardDtos.Storage storage,
            Activity activity,
            Errors errors
    ) {}
}
