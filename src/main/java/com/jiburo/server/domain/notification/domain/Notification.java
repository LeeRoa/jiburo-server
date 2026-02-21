package com.jiburo.server.domain.notification.domain;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_receiver", columnList = "receiver_id"),
        @Index(name = "idx_notification_is_read", columnList = "is_read")
})
public class Notification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    @Comment("알림 수신자")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @Comment("알림 발신자 (시스템 알림일 경우 null)")
    private User sender;

    @Column(nullable = false, length = 20)
    @Comment("알림 유형 코드 (CHAT, COMMENT, WALK_AREA 등)")
    private String typeCode;

    @Column
    @Comment("이동 타겟 ID (게시글 ID, 채팅방 ID 등)")
    private Long targetId;

    @Column(nullable = false)
    @Comment("읽음 여부")
    private boolean isRead;

    @Builder
    public Notification(User receiver, User sender, String typeCode, String messageKey, Long targetId) {
        this.receiver = receiver;
        this.sender = sender;
        this.typeCode = typeCode;
        this.targetId = targetId;
        this.isRead = false;
    }

    // --- 비즈니스 로직 ---

    /**
     * 알림 읽음 처리
     */
    public void read() {
        this.isRead = true;
    }
}
