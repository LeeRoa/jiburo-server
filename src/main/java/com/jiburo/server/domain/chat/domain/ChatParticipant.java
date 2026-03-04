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
@Table(name = "chat_participants", indexes = {
        @Index(name = "idx_participant_user", columnList = "user_id"),
        @Index(name = "idx_participant_room", columnList = "chat_room_id")
})
public class ChatParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("참여자 테이블 PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @Comment("참여 중인 채팅방 참조")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("채팅방에 속한 유저 정보")
    private User user;

    @Column(nullable = false)
    @Comment("해당 유저가 방을 나갔는지 여부 (true일 경우 목록에서 제외하는 Soft Delete 방식)")
    private boolean isExited = false;

    @Comment("해당 유저가 마지막으로 읽은 메시지 PK (ID)")
    private Long lastReadMessageId = 0L;

    @Builder
    public ChatParticipant(ChatRoom chatRoom, User user) {
        this.chatRoom = chatRoom;
        this.user = user;
    }

    /**
     * [연관관계 편의 메서드]
     * 빌더나 생성자 외에 수동으로 방 정보를 세팅할 때 사용합니다.
     */
    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        if (!chatRoom.getParticipants().contains(this)) {
            chatRoom.getParticipants().add(this);
        }
    }

    /**
     * 방 나가기 처리 (데이터 보존을 위해 삭제 대신 플래그 변경)
     */
    public void exit() {
        this.isExited = true;
    }

    /**
     * 읽음 시점 최신화 (과거 메시지 ID가 들어오면 무시하도록 방어 로직 추가)
     */
    public void updateLastRead(Long messageId) {
        if (messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }
}
