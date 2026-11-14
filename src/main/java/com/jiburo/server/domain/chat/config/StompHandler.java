package com.jiburo.server.domain.chat.config;

import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.jwt.JwtFilter;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 인바운드 인가 처리
 * - CONNECT   : Authorization 헤더의 Access Token 필수 (없거나 무효/로그아웃 토큰이면 거부)
 * - SUBSCRIBE : /sub/chat/rooms/{roomId}[/read] 만 허용하며, 해당 방의 (나가지 않은) 참여자만 구독 가능
 * - SEND      : 인증된 세션 + /pub/** destination만 허용.
 *               /sub/** 로 직접 SEND하면 SimpleBroker가 검증 없이 구독자에게 그대로 뿌리므로 반드시 막는다.
 *               CONNECT 때 받은 토큰이 만료됐거나 로그아웃(블랙리스트)됐으면 거부한다 (소켓은 연결 후 계속 살아 있으므로 SEND마다 재확인).
 *               방 참여자·정지 여부 검증은 ChatMessageService에서 수행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    static final Pattern CHAT_ROOM_DESTINATION = Pattern.compile("^/sub/chat/rooms/([A-Za-z0-9]+)(/read)?$");
    private static final String APP_DESTINATION_PREFIX = "/pub/";

    // CONNECT 때 세션에 남겨 두고 SEND마다 재확인하는 값
    static final String SESSION_ACCESS_TOKEN = "jiburo.accessToken";
    static final String SESSION_TOKEN_EXPIRES_AT = "jiburo.tokenExpiresAt";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenService authTokenService;
    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            case SEND -> authorizeSend(accessor);
            default -> { /* DISCONNECT, UNSUBSCRIBE, ACK 등은 통과 */ }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(JwtFilter.AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(JwtFilter.BEARER_PREFIX)) {
            log.warn("STOMP 인증 실패: Authorization 헤더 없음");
            throw new JiburoException(ErrorCode.UNAUTHORIZED);
        }

        String token = header.substring(JwtFilter.BEARER_PREFIX.length());
        if (!jwtTokenProvider.validateAccessToken(token) || authTokenService.isBlacklisted(token)) {
            log.warn("STOMP 인증 실패: 유효하지 않은 JWT 토큰");
            throw new JiburoException(ErrorCode.INVALID_TOKEN);
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        accessor.setUser(authentication);

        Map<String, Object> session = accessor.getSessionAttributes();
        if (session != null) {
            session.put(SESSION_ACCESS_TOKEN, token);
            session.put(SESSION_TOKEN_EXPIRES_AT, System.currentTimeMillis() + jwtTokenProvider.getRemainingValidityMillis(token));
        }
        log.debug("STOMP 인증 성공: {}", authentication.getName());
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        UUID userId = requireUserId(accessor);
        String destination = accessor.getDestination();

        Matcher matcher = destination != null ? CHAT_ROOM_DESTINATION.matcher(destination) : null;
        if (matcher == null || !matcher.matches()) {
            log.warn("STOMP 구독 거부: 허용되지 않은 destination={}, userId={}", destination, userId);
            throw new JiburoException(ErrorCode.ACCESS_DENIED);
        }

        Long roomId = HashidsUtils.decode(matcher.group(1));
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndIsExitedFalse(roomId, userId)) {
            log.warn("STOMP 구독 거부: 채팅방 참여자 아님 roomId={}, userId={}", roomId, userId);
            throw new JiburoException(ErrorCode.NOT_CHAT_PARTICIPANT);
        }
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        UUID userId = requireUserId(accessor);

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(APP_DESTINATION_PREFIX)) {
            log.warn("STOMP 발행 거부: 허용되지 않은 destination={}, userId={}", destination, userId);
            throw new JiburoException(ErrorCode.ACCESS_DENIED);
        }

        Map<String, Object> session = accessor.getSessionAttributes();
        if (session == null) {
            return;
        }
        if (session.get(SESSION_TOKEN_EXPIRES_AT) instanceof Long expiresAt && System.currentTimeMillis() >= expiresAt) {
            log.debug("STOMP 발행 거부: 연결 토큰 만료 userId={}", userId);
            throw new JiburoException(ErrorCode.INVALID_TOKEN);
        }
        if (session.get(SESSION_ACCESS_TOKEN) instanceof String token && authTokenService.isBlacklisted(token)) {
            log.debug("STOMP 발행 거부: 로그아웃된 토큰 userId={}", userId);
            throw new JiburoException(ErrorCode.INVALID_TOKEN);
        }
    }

    private UUID requireUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return user.getUserId();
        }
        throw new JiburoException(ErrorCode.UNAUTHORIZED);
    }
}
