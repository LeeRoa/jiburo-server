package com.jiburo.server.domain.chat.config;

import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatSubscriptionRevokerTest {

    private final SimpUserRegistry userRegistry = mock(SimpUserRegistry.class);
    private final MessageChannel brokerChannel = mock(MessageChannel.class);
    private final ChatSubscriptionRevoker revoker = new ChatSubscriptionRevoker(userRegistry, brokerChannel);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
    }

    private SimpSubscription subscription(String id, String destination) {
        SimpSubscription sub = mock(SimpSubscription.class);
        given(sub.getId()).willReturn(id);
        given(sub.getDestination()).willReturn(destination);
        return sub;
    }

    @Test
    void 나간_방의_메시지_읽음_구독만_브로커에서_해제한다() {
        String room10 = HashidsUtils.encode(10L);
        String room11 = HashidsUtils.encode(11L);
        SimpSubscription msgSub = subscription("s1", "/sub/chat/rooms/" + room10);
        SimpSubscription readSub = subscription("s2", "/sub/chat/rooms/" + room10 + "/read");
        SimpSubscription otherRoom = subscription("s3", "/sub/chat/rooms/" + room11);

        SimpSession session = mock(SimpSession.class);
        given(session.getId()).willReturn("session-1");
        given(session.getSubscriptions()).willReturn(Set.of(msgSub, readSub, otherRoom));
        SimpUser user = mock(SimpUser.class);
        given(user.getSessions()).willReturn(Set.of(session));
        given(userRegistry.getUser(userId.toString())).willReturn(user);
        given(brokerChannel.send(any())).willReturn(true);

        revoker.revokeRoom(userId, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(brokerChannel, times(2)).send(captor.capture());
        List<String> revoked = captor.getAllValues().stream()
                .peek(m -> {
                    assertThat(SimpMessageHeaderAccessor.getMessageType(m.getHeaders())).isEqualTo(SimpMessageType.UNSUBSCRIBE);
                    assertThat(SimpMessageHeaderAccessor.getSessionId(m.getHeaders())).isEqualTo("session-1");
                })
                .map(m -> SimpMessageHeaderAccessor.getSubscriptionId(m.getHeaders()))
                .toList();
        assertThat(revoked).containsExactlyInAnyOrder("s1", "s2");
    }

    @Test
    void 접속_중이_아니면_아무것도_하지_않는다() {
        given(userRegistry.getUser(userId.toString())).willReturn(null);

        revoker.revokeRoom(userId, 10L);

        verify(brokerChannel, never()).send(any());
    }
}
