package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks private ChatMessageServiceImpl chatMessageService;

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatParticipantRepository chatParticipantRepository;

    private final Long roomId = 1L;
    private UUID senderId;
    private ChatRoom room;
    private ChatParticipant senderPart;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        senderId = UUID.randomUUID();
        User sender = User.builder().oauthId("kakao_1").nickname("sender").roleCode(RoleType.USER).build();
        ReflectionTestUtils.setField(sender, "id", senderId);

        room = ChatRoom.builder().host(sender).build();
        ReflectionTestUtils.setField(room, "id", roomId);
        senderPart = new ChatParticipant(room, sender);
    }

    private void stubRoomAndParticipant() {
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(chatParticipantRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.of(senderPart));
    }

    private void stubSaveWithId(long id) {
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            ReflectionTestUtils.setField(msg, "id", id);
            return msg;
        });
    }

    @Test
    void 메시지를_보내면_보낸_사람의_읽음_커서가_해당_메시지로_이동한다() {
        stubRoomAndParticipant();
        stubSaveWithId(42L);

        chatMessageService.saveMessage(roomId, senderId, new ChatMessageRequestDto("안녕하세요", ChatMsgType.TALK));

        assertThat(senderPart.getLastReadMessageId()).isEqualTo(42L);
        assertThat(room.getLastMessage()).isEqualTo("안녕하세요");
    }

    @Test
    void 이미지_메시지는_목록_미리보기에_파일키_대신_타입표기가_저장된다() {
        stubRoomAndParticipant();
        stubSaveWithId(43L);

        chatMessageService.saveMessage(roomId, senderId,
                new ChatMessageRequestDto("chat/images/uuid/abc.png", ChatMsgType.IMAGE));

        assertThat(room.getLastMessage()).isEqualTo("[IMAGE]");
    }

    @Test
    void 텍스트가_1000자를_넘으면_INVALID_INPUT_VALUE() {
        stubRoomAndParticipant();

        assertThatThrownBy(() -> chatMessageService.saveMessage(roomId, senderId,
                new ChatMessageRequestDto("a".repeat(1001), ChatMsgType.TALK)))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void 내용이나_타입이_비어있으면_INVALID_INPUT_VALUE() {
        stubRoomAndParticipant();

        assertThatThrownBy(() -> chatMessageService.saveMessage(roomId, senderId, new ChatMessageRequestDto(" ", ChatMsgType.TALK)))
                .isInstanceOf(JiburoException.class);
        assertThatThrownBy(() -> chatMessageService.saveMessage(roomId, senderId, new ChatMessageRequestDto("hi", null)))
                .isInstanceOf(JiburoException.class);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void 나간_참여자는_메시지를_보낼_수_없다() {
        senderPart.exit();
        stubRoomAndParticipant();

        assertThatThrownBy(() -> chatMessageService.saveMessage(roomId, senderId, new ChatMessageRequestDto("hi", ChatMsgType.TALK)))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_CHAT_PARTICIPANT));
    }
}
