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
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher; // 푸시 이벤트
    @org.mockito.Spy private com.jiburo.server.domain.moderation.service.BannedWordFilter bannedWordFilter =
            new com.jiburo.server.domain.moderation.service.BannedWordFilter(org.mockito.Mockito.mock(com.jiburo.server.domain.moderation.repository.BannedWordRepository.class)); // 금지어 없음 = 그대로 통과
    @org.mockito.Spy private com.jiburo.server.global.setting.AppSettingService appSettingService =
            com.jiburo.server.global.setting.TestSettings.defaults();

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
    void 관리자가_최대_길이를_줄이면_그_길이를_넘는_메시지는_거부된다() {
        com.jiburo.server.global.setting.TestSettings.with(appSettingService,
                com.jiburo.server.global.setting.SettingKey.CHAT_MAX_TEXT_LENGTH, "10");
        stubRoomAndParticipant();

        assertThatThrownBy(() -> chatMessageService.saveMessage(roomId, senderId,
                new ChatMessageRequestDto("a".repeat(11), ChatMsgType.TALK)))
                .isInstanceOf(JiburoException.class);
        verify(chatMessageRepository, never()).save(any());
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

    @Test
    void 정지된_사용자는_연결된_소켓으로도_메시지를_보낼_수_없다() {
        senderPart.getUser().suspend(java.time.LocalDateTime.now().plusDays(1), "spam");
        stubRoomAndParticipant();

        assertThatThrownBy(() -> chatMessageService.saveMessage(roomId, senderId, new ChatMessageRequestDto("hi", ChatMsgType.TALK)))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_SUSPENDED));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void 안_읽음_수에서_나간_참여자는_빠진다() {
        User partner = User.builder().oauthId("kakao_2").nickname("partner").roleCode(RoleType.USER).build();
        ReflectionTestUtils.setField(partner, "id", UUID.randomUUID());
        User exited = User.builder().oauthId("kakao_3").nickname("exited").roleCode(RoleType.USER).build();
        ReflectionTestUtils.setField(exited, "id", UUID.randomUUID());
        ChatParticipant exitedPart = new ChatParticipant(room, exited);
        exitedPart.exit();
        senderPart.setChatRoom(room);
        new ChatParticipant(room, partner).setChatRoom(room);
        exitedPart.setChatRoom(room);
        stubRoomAndParticipant();
        stubSaveWithId(50L);

        var response = chatMessageService.saveMessage(roomId, senderId, new ChatMessageRequestDto("hi", ChatMsgType.TALK));

        assertThat(response.unreadCount()).isEqualTo(1);
    }

    // ───────── processReadReceipt ─────────

    @Test
    void 다른_방의_메시지_ID로는_읽음_처리할_수_없다() {
        given(chatParticipantRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.of(senderPart));
        given(chatMessageRepository.existsByIdAndChatRoomId(999L, roomId)).willReturn(false);

        assertThatThrownBy(() -> chatMessageService.processReadReceipt(senderId, roomId, 999L))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        assertThat(senderPart.getLastReadMessageId()).isZero();
    }

    @Test
    void 예전_메시지로_읽음_처리해도_커서는_뒤로_가지_않고_현재_커서로_알린다() {
        senderPart.updateLastRead(30L);
        given(chatParticipantRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.of(senderPart));
        given(chatMessageRepository.existsByIdAndChatRoomId(10L, roomId)).willReturn(true);

        var response = chatMessageService.processReadReceipt(senderId, roomId, 10L);

        assertThat(senderPart.getLastReadMessageId()).isEqualTo(30L);
        assertThat(response.lastReadMessageId()).isEqualTo(HashidsUtils.encode(30L));
    }
}
