package com.jiburo.server.domain.chat.domain;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

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

    @Column(name = "message_type_code", length = 20, nullable = false)
    @Comment("메시지 타입 코드 (common_codes 참조: CHAT_TYPE_TALK, CHAT_TYPE_IMAGE 등)")
    private String messageTypeCode;

    @Builder
    public ChatMessage(ChatRoom chatRoom, User sender, String content, String messageTypeCode) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.messageTypeCode = messageTypeCode;
    }
}
