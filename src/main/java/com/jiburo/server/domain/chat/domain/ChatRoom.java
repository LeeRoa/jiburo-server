package com.jiburo.server.domain.chat.domain;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms", indexes = {
        @Index(name = "idx_chatroom_post", columnList = "post_id"),
        @Index(name = "idx_chatroom_last_chat", columnList = "last_chat_at") // 최신순 정렬 최적화
})
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("채팅방 고유 식별자 (외부 노출 시 Hashids로 인코딩)")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @Comment("연결된 실종/제보 게시글 ID (어떤 게시물로 시작된 대화인지 추적)")
    private LostPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    @Comment("방장 유저 ID (방의 생성자이자 추후 권한 관리의 기준)")
    private User host;

    @Column(length = 1000)
    @Comment("목록 조회 최적화를 위해 저장하는 마지막 채팅 메시지 내용")
    private String lastMessage;

    @Comment("마지막 메시지가 전송된 시각 (채팅 목록의 정렬 기준으로 사용)")
    private LocalDateTime lastChatAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ChatParticipant> participants = new ArrayList<>();

    @Builder
    public ChatRoom(LostPost post, User host) {
        this.post = post;
        this.host = host;
        this.lastChatAt = LocalDateTime.now(); // 생성 시점 초기화
    }

    /**
     * 새로운 메시지가 발생했을 때 방의 상태(마지막 메시지, 시간)를 최신화합니다.
     */
    public void updateLastChat(String message) {
        this.lastMessage = message;
        this.lastChatAt = LocalDateTime.now();
    }

    /**
     * [연관관계 편의 메서드]
     * 채팅방에 참여자를 추가할 때, 참여자 객체에도 채팅방 정보를 자동으로 세팅해줍니다.
     * 1:N 양방향 관계에서 데이터 정합성을 맞추기 위한 필수 관례입니다.
     */
    public void addParticipant(ChatParticipant participant) {
        this.participants.add(participant);
        if (participant.getChatRoom() != this) {
            participant.setChatRoom(this);
        }
    }
}
