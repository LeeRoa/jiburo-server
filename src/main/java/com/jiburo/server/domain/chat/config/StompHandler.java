package com.jiburo.server.domain.chat.config;

import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.error.BusinessException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 1. 연결 요청(CONNECT)일 때만 토큰 검증
        if (StompCommand.CONNECT == Objects.requireNonNull(accessor).getCommand()) {
            String jwt = accessor.getFirstNativeHeader("Authorization");
            log.debug("STOMP 연결 시도 - JWT: {}", jwt);

            if (jwt != null && jwt.startsWith("Bearer ")) {
                String token = jwt.substring(7);

                // 2. JWT 검증
                if (jwtTokenProvider.validateToken(token)) {
                    // 3. 인증 객체 생성 및 세션에 저장
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    accessor.setUser(authentication);
                    log.debug("STOMP 인증 성공: {}", authentication.getName());
                } else {
                    log.error("STOMP 인증 실패: 유효하지 않은 JWT 토큰입니다.");
                    throw new BusinessException(ErrorCode.INVALID_TOKEN);
                }
            }
        }
        return message;
    }
}
