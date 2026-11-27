package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @Mock private ChatParticipantRepository chatParticipantRepository;

    // ChatRoomServiceImpl이 의존하는 나머지 빈들 — exitChatRoom에서 사용 안 하므로 Mock만 선언
    @Mock private com.jiburo.server.domain.chat.repository.ChatRoomRepository chatRoomRepository;
    @Mock private com.jiburo.server.domain.chat.repository.ChatMessageRepository chatMessageRepository;
    @Mock private com.jiburo.server.domain.post.repository.LostPostRepository lostPostRepository;
    @Mock private com.jiburo.server.domain.user.dao.UserRepository userRepository;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock private com.jiburo.server.domain.chat.config.ChatSubscriptionRevoker chatSubscriptionRevoker;

    private UUID userId;
    private Long roomId;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        userId = UUID.randomUUID();
        roomId = 1L;
    }

    // ───────── exitChatRoom ─────────

    @Test
    void 채팅방_나가기_성공() {
        // given
        ChatParticipant participant = mock(ChatParticipant.class);
        given(chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId))
                .willReturn(Optional.of(participant));

        // when
        chatRoomService.exitChatRoom(roomId, userId);

        // then
        then(participant).should().exit();
        then(chatSubscriptionRevoker).should().revokeRoomAfterCommit(userId, roomId); // 기존 구독도 회수
    }

    @Test
    void 참여자가_아니면_NOT_CHAT_PARTICIPANT_예외() {
        // given
        given(chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.exitChatRoom(roomId, userId))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex ->
                        assertThat(((JiburoException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.NOT_CHAT_PARTICIPANT)
                );
    }
}
