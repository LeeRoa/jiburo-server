package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.report.domain.Report;
import com.jiburo.server.domain.report.domain.enums.ReportReason;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.domain.enums.ReportTargetType;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminReportDtos {

    private AdminReportDtos() {
    }

    public enum ContentState {
        @Schema(description = "원본이 남아 있음") EXISTS,
        @Schema(description = "원본이 삭제됨 (작성자 삭제 또는 관리자 삭제)") DELETED,
        @Schema(description = "사용자 신고라 콘텐츠 없음") NONE
    }

    public record SearchCondition(
            @Schema(description = "처리 상태 (기본: 전체)") ReportStatus status,
            @Schema(description = "대상 종류") ReportTargetType targetType,
            @Schema(description = "신고 사유") ReportReason reason,
            @Schema(description = "신고당한 사용자 ID") UUID targetUserId
    ) {}

    public record ReportSummary(
            @Schema(description = "신고 ID (인코딩된 문자열)") String id,
            @Schema(description = "대상 종류") ReportTargetType targetType,
            @Schema(description = "대상 ID — POST·CHAT_MESSAGE는 인코딩된 문자열, USER는 UUID") String targetId,
            @Schema(description = "신고 사유") ReportReason reason,
            @Schema(description = "신고자 상세 내용") String detail,
            @Schema(description = "신고자 ID") UUID reporterId,
            @Schema(description = "신고자 닉네임") String reporterNickname,
            @Schema(description = "신고당한 사용자 ID") UUID targetUserId,
            @Schema(description = "신고당한 사용자 닉네임") String targetUserNickname,
            @Schema(description = "신고 당시 내용") String contentSnapshot,
            @Schema(description = "처리 상태") ReportStatus status,
            @Schema(description = "같은 대상에 쌓인 처리 대기 신고 수 (이 신고 포함)") long pendingCountForTarget,
            @Schema(description = "신고 시각") LocalDateTime createdAt,
            @Schema(description = "처리 시각") LocalDateTime handledAt
    ) {
        public static ReportSummary of(Report r, long pendingCountForTarget) {
            String targetId = switch (r.getTargetType()) {
                case POST -> HashidsUtils.encode(r.getTargetPostId());
                case CHAT_MESSAGE -> HashidsUtils.encode(r.getTargetMessageId());
                case USER -> r.getTargetUser().getId().toString();
            };
            return new ReportSummary(HashidsUtils.encode(r.getId()), r.getTargetType(), targetId, r.getReason(), r.getDetail(),
                    r.getReporter().getId(), r.getReporter().getNickname(),
                    r.getTargetUser().getId(), r.getTargetUser().getNickname(),
                    r.getContentSnapshot(), r.getStatus(), pendingCountForTarget, r.getCreatedAt(), r.getHandledAt());
        }
    }

    public record ContextMessage(
            @Schema(description = "메시지 ID (인코딩된 문자열)") String id,
            @Schema(description = "보낸 사람 닉네임") String senderNickname,
            @Schema(description = "메시지 타입") ChatMsgType type,
            @Schema(description = "내용 (이미지 등은 파일 키)") String content,
            @Schema(description = "보낸 시각") LocalDateTime createdAt,
            @Schema(description = "신고된 메시지인지") boolean reported
    ) {
        public static ContextMessage of(ChatMessage m, Long reportedId) {
            return new ContextMessage(HashidsUtils.encode(m.getId()), m.getSender().getNickname(), m.getMessageTypeCode(),
                    m.getContent(), m.getCreatedAt(), m.getId().equals(reportedId));
        }
    }

    public record ReportDetail(
            @Schema(description = "요약") ReportSummary summary,
            @Schema(description = "원본 상태") ContentState contentState,
            @Schema(description = "채팅 메시지 신고일 때 앞뒤 대화 (최대 5개씩)") List<ContextMessage> chatContext,
            @Schema(description = "신고당한 사용자가 받은 전체 신고 수") long targetUserReportCount,
            @Schema(description = "신고당한 사용자의 현재 정지 만료 시각 (null = 정지 아님)") LocalDateTime targetUserSuspendedUntil,
            @Schema(description = "처리한 관리자 ID") UUID handledBy,
            @Schema(description = "처리한 관리자 닉네임") String handledByNickname,
            @Schema(description = "처리 시 콘텐츠를 삭제했는지") boolean contentDeleted,
            @Schema(description = "처리 시 내린 정지 만료 시각") LocalDateTime suspendedUntil,
            @Schema(description = "처리 메모") String adminNote
    ) {}

    public record ResolveRequest(
            @Schema(description = "콘텐츠 삭제 (게시글 강제 삭제 / 채팅 메시지 가리기). 사용자 신고에는 쓸 수 없음") boolean deleteContent,
            @Schema(description = "정지 일수 (1~3650). suspendPermanent와 함께 보내면 영구가 우선") @Min(1) @Max(3650) Integer suspendDays,
            @Schema(description = "영구 정지") boolean suspendPermanent,
            @Schema(description = "처리 메모 (관리자 전용, 정지 사유로도 저장)") @Size(max = 500) String note
    ) {
        public boolean suspend() {
            return suspendPermanent || suspendDays != null;
        }
    }

    public record ResolveResult(
            @Schema(description = "결과 상태 (조치가 하나라도 있으면 RESOLVED, 없으면 DISMISSED)") ReportStatus status,
            @Schema(description = "같은 대상이라 함께 처리된 신고 수 (이 신고 포함)") int handledCount,
            @Schema(description = "정지 만료 시각 (정지하지 않았으면 null)") LocalDateTime suspendedUntil
    ) {}

    public record SuspendRequest(
            @Schema(description = "정지 일수 (1~3650, 비우면 영구 정지)") @Min(1) @Max(3650) Integer days,
            @Schema(description = "정지 사유 (관리자 전용)") @Size(max = 500) String reason
    ) {}
}
