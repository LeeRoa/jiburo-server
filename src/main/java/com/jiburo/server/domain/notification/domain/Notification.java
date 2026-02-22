package com.jiburo.server.domain.notification.domain;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 사용자에게 발송되는 알림 정보를 관리하는 엔티티
 * 실시간 웹소켓 푸시 및 알림 내역 조회에 사용됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications", indexes = {
        @Index(name = "idx_noti_receiver", columnList = "receiver_id"),
        @Index(name = "idx_noti_created_at", columnList = "created_at")
})
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("알림 고유 식별자")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    @Comment("알림을 받는 수신자 (회원)")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @Comment("알림 발신자 (시스템 알림일 경우 null)")
    private User sender;

    @Column(nullable = false, length = 30)
    @Comment("알림 유형 코드 (예: NOTI_CHAT, NOTI_POST_LIKE / 프론트엔드 다국어 키와 매핑)")
    private String typeCode;

    @Column(length = 255)
    @Comment("알림 메시지 구성을 위한 가변 인자 (콤마로 구분, 예: 닉네임, 게시글제목)")
    private String args;

    @Column
    @Comment("알림 클릭 시 이동할 목적지 ID (게시글 ID, 채팅방 ID 등)")
    private Long targetId;

    @Column(nullable = false)
    @Comment("알림 읽음 여부 (true: 읽음, false: 안읽음)")
    private boolean isRead;

    @Builder
    public Notification(User receiver, User sender, String typeCode, String args, Long targetId) {
        this.receiver = receiver;
        this.sender = sender;
        this.typeCode = typeCode;
        this.args = args;
        this.targetId = targetId;
        this.isRead = false;
    }

    // --- 비즈니스 로직 ---

    /**
     * 알림을 읽음 상태로 변경합니다.
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
