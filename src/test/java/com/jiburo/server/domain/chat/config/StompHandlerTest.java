package com.jiburo.server.domain.chat.config;

import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

    @InjectMocks private StompHandler stompHandler;

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthTokenService authTokenService;
    @Mock private ChatParticipantRepository chatParticipantRepository;

    private final MessageChannel channel = mock(MessageChannel.class);
    private UUID userId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        userId = UUID.randomUUID();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        authentication = new UsernamePasswordAuthenticationToken(new CustomOAuth2User(userId, authorities), "", authorities);
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void assertError(Message<?> msg, ErrorCode expected) {
        assertThatThrownBy(() -> stompHandler.preSend(msg, channel))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(expected));
    }

    // ───────── CONNECT ─────────

    @Test
    void Authorization_헤더_없이_CONNECT하면_UNAUTHORIZED() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertError(message(accessor), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 블랙리스트_토큰으로_CONNECT하면_INVALID_TOKEN() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer logged.out.token");
        given(jwtTokenProvider.validateAccessToken("logged.out.token")).willReturn(true);
        given(authTokenService.isBlacklisted("logged.out.token")).willReturn(true);

        assertError(message(accessor), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 유효한_토큰으로_CONNECT하면_세션에_사용자가_설정된다() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer good.token");
        given(jwtTokenProvider.validateAccessToken("good.token")).willReturn(true);
        given(authTokenService.isBlacklisted("good.token")).willReturn(false);
        given(jwtTokenProvider.getAuthentication("good.token")).willReturn(authentication);

        stompHandler.preSend(message(accessor), channel);

        assertThat(accessor.getUser()).isEqualTo(authentication);
    }

    // ───────── SUBSCRIBE ─────────

    private StompHeaderAccessor subscribe(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(authentication);
        return accessor;
    }

    @Test
    void 참여자가_아닌_방을_구독하면_NOT_CHAT_PARTICIPANT() {
        String roomHash = HashidsUtils.encode(10L);
        given(chatParticipantRepository.existsByChatRoomIdAndUserIdAndIsExitedFalse(10L, userId)).willReturn(false);

        assertError(message(subscribe("/sub/chat/rooms/" + roomHash)), ErrorCode.NOT_CHAT_PARTICIPANT);
    }

    @Test
    void 참여자는_메시지와_읽음_채널을_구독할_수_있다() {
        String roomHash = HashidsUtils.encode(10L);
        given(chatParticipantRepository.existsByChatRoomIdAndUserIdAndIsExitedFalse(10L, userId)).willReturn(true);

        assertThatNoException().isThrownBy(() -> stompHandler.preSend(message(subscribe("/sub/chat/rooms/" + roomHash)), channel));
        assertThatNoException().isThrownBy(() -> stompHandler.preSend(message(subscribe("/sub/chat/rooms/" + roomHash + "/read")), channel));
    }

    @Test
    void 허용되지_않은_destination_구독은_ACCESS_DENIED() {
        assertError(message(subscribe("/sub/chat/rooms/**")), ErrorCode.ACCESS_DENIED);
        assertError(message(subscribe("/sub/notifications")), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void 인증_없는_세션의_SUBSCRIBE_SEND는_UNAUTHORIZED() {
        StompHeaderAccessor sub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        sub.setDestination("/sub/chat/rooms/" + HashidsUtils.encode(10L));
        assertError(message(sub), ErrorCode.UNAUTHORIZED);

        StompHeaderAccessor send = StompHeaderAccessor.create(StompCommand.SEND);
        send.setDestination("/pub/chat/rooms/" + HashidsUtils.encode(10L));
        assertError(message(send), ErrorCode.UNAUTHORIZED);
    }

    // ───────── SEND ─────────

    private StompHeaderAccessor send(String destination, Map<String, Object> session) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination(destination);
        accessor.setUser(authentication);
        accessor.setSessionAttributes(session);
        return accessor;
    }

    private Map<String, Object> liveSession(String token) {
        Map<String, Object> session = new HashMap<>();
        session.put(StompHandler.SESSION_ACCESS_TOKEN, token);
        session.put(StompHandler.SESSION_TOKEN_EXPIRES_AT, System.currentTimeMillis() + 60_000L);
        return session;
    }

    @Test
    void 브로커_destination으로_직접_SEND하면_ACCESS_DENIED() {
        // /sub 로 바로 보내면 SimpleBroker가 참여자·금지어 검증 없이 구독자에게 뿌리므로 막아야 한다
        String roomHash = HashidsUtils.encode(10L);
        assertError(message(send("/sub/chat/rooms/" + roomHash, liveSession("t"))), ErrorCode.ACCESS_DENIED);
        assertError(message(send("/sub/chat/rooms/" + roomHash + "/read", liveSession("t"))), ErrorCode.ACCESS_DENIED);
        assertError(message(send(null, liveSession("t"))), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void 유효한_세션은_pub으로_SEND할_수_있다() {
        given(authTokenService.isBlacklisted("t")).willReturn(false);

        assertThatNoException().isThrownBy(() ->
                stompHandler.preSend(message(send("/pub/chat/rooms/" + HashidsUtils.encode(10L), liveSession("t"))), channel));
    }

    @Test
    void 연결_토큰이_만료되면_SEND는_INVALID_TOKEN() {
        Map<String, Object> session = liveSession("t");
        session.put(StompHandler.SESSION_TOKEN_EXPIRES_AT, System.currentTimeMillis() - 1L);

        assertError(message(send("/pub/chat/rooms/" + HashidsUtils.encode(10L), session)), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 로그아웃한_토큰의_세션은_SEND가_INVALID_TOKEN() {
        given(authTokenService.isBlacklisted("t")).willReturn(true);

        assertError(message(send("/pub/chat/rooms/" + HashidsUtils.encode(10L), liveSession("t"))), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void CONNECT하면_세션에_토큰과_만료시각이_저장된다() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer good.token");
        Map<String, Object> session = new HashMap<>();
        accessor.setSessionAttributes(session);
        given(jwtTokenProvider.validateAccessToken("good.token")).willReturn(true);
        given(authTokenService.isBlacklisted("good.token")).willReturn(false);
        given(jwtTokenProvider.getAuthentication("good.token")).willReturn(authentication);
        given(jwtTokenProvider.getRemainingValidityMillis("good.token")).willReturn(60_000L);

        stompHandler.preSend(message(accessor), channel);

        assertThat(session.get(StompHandler.SESSION_ACCESS_TOKEN)).isEqualTo("good.token");
        assertThat((Long) session.get(StompHandler.SESSION_TOKEN_EXPIRES_AT)).isGreaterThan(System.currentTimeMillis());
    }
}
