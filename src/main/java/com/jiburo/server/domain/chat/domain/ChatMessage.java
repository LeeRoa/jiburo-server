package com.jiburo.server.domain.chat.domain;

import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_room_created", columnList = "chat_room_id, created_at") // 최적화의 핵심
})
public class ChatMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("메시지 PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @Comment("어느 방에서 오간 대화인지 참조")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @Comment("메시지를 보낸 사람")
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Comment("메시지 내용 (최대 1000자 권장)")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type_code", length = 20, nullable = false)
    @Comment("메시지 타입 코드 (common_codes 참조: CHAT_TYPE_TALK, CHAT_TYPE_IMAGE 등)")
    private ChatMsgType messageTypeCode;

    @Builder
    public ChatMessage(ChatRoom chatRoom, User sender, String content, ChatMsgType messageTypeCode) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.messageTypeCode = messageTypeCode;
    }

    /** 신고 처리로 관리자가 지운 메시지 — SYS 타입 + 고정 키로 바꿔 원문을 남기지 않는다 (프론트가 다국어 문구로 표시) */
    public static final String DELETED_BY_ADMIN = "chat.msg.deleted_by_admin";

    public void deleteByAdmin() {
        this.content = DELETED_BY_ADMIN;
        this.messageTypeCode = ChatMsgType.SYS;
    }

    public boolean isDeletedByAdmin() {
        return messageTypeCode == ChatMsgType.SYS && DELETED_BY_ADMIN.equals(content);
    }
}
