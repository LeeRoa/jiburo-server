package com.jiburo.server.domain.report.domain;

import com.jiburo.server.domain.report.domain.enums.ReportReason;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.domain.enums.ReportTargetType;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 신고.
 * - target_key("POST:12", "CHAT_MESSAGE:34", "USER:<uuid>")로 같은 대상의 신고를 묶는다 (중복 신고 차단, 일괄 처리).
 * - 처리 시점에 원문이 지워져 있어도 판단할 수 있도록 신고 당시 내용을 content_snapshot에 남긴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status_created", columnList = "status, created_at"),
        @Index(name = "idx_report_target_key", columnList = "target_key, status"),
        @Index(name = "idx_report_target_user", columnList = "target_user_id")
})
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    @Comment("신고한 사용자")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    @Comment("신고 대상 종류")
    private ReportTargetType targetType;

    @Column(name = "target_post_id")
    @Comment("대상 게시글 ID (POST)")
    private Long targetPostId;

    @Column(name = "target_message_id")
    @Comment("대상 채팅 메시지 ID (CHAT_MESSAGE)")
    private Long targetMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    @Comment("신고당한 사용자 (게시글 작성자 / 메시지 발신자 / 신고된 사용자)")
    private User targetUser;

    @Column(name = "target_key", nullable = false, length = 80)
    @Comment("같은 대상 묶음 키")
    private String targetKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("신고 사유")
    private ReportReason reason;

    @Column(length = 500)
    @Comment("신고자가 적은 상세 내용")
    private String detail;

    @Column(name = "content_snapshot", columnDefinition = "TEXT")
    @Comment("신고 당시 대상 내용 (제목+본문 / 메시지 / 닉네임)")
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("처리 상태")
    private ReportStatus status;

    // ── 처리 결과 ──
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "handled_by")
    @Comment("처리한 관리자 ID")
    private UUID handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "content_deleted", nullable = false)
    @Comment("처리 시 콘텐츠 삭제 여부")
    private boolean contentDeleted;

    @Column(name = "suspended_until")
    @Comment("처리 시 내린 정지 만료 시각 (null = 정지 없음)")
    private LocalDateTime suspendedUntil;

    @Column(name = "admin_note", length = 500)
    @Comment("처리 메모 (관리자 전용)")
    private String adminNote;

    @Builder
    public Report(User reporter, ReportTargetType targetType, Long targetPostId, Long targetMessageId,
                  User targetUser, ReportReason reason, String detail, String contentSnapshot) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetPostId = targetPostId;
        this.targetMessageId = targetMessageId;
        this.targetUser = targetUser;
        this.targetKey = keyOf(targetType, targetPostId, targetMessageId, targetUser.getId());
        this.reason = reason;
        this.detail = detail;
        this.contentSnapshot = contentSnapshot;
        this.status = ReportStatus.PENDING;
    }

    public static String keyOf(ReportTargetType type, Long postId, Long messageId, UUID userId) {
        return switch (type) {
            case POST -> "POST:" + postId;
            case CHAT_MESSAGE -> "CHAT_MESSAGE:" + messageId;
            case USER -> "USER:" + userId;
        };
    }

    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }

    public void handle(UUID adminId, boolean contentDeleted, LocalDateTime suspendedUntil, String note) {
        this.status = contentDeleted || suspendedUntil != null ? ReportStatus.RESOLVED : ReportStatus.DISMISSED;
        this.handledBy = adminId;
        this.handledAt = LocalDateTime.now();
        this.contentDeleted = contentDeleted;
        this.suspendedUntil = suspendedUntil;
        this.adminNote = note;
    }
}
