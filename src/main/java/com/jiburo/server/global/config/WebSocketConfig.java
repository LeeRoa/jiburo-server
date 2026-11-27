package com.jiburo.server.global.config;

import com.jiburo.server.domain.chat.config.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    /**
     * WebSocket 연결을 허용할 브라우저 origin — REST CORS와 같은 목록(app.cors.allowed-origins, 운영은 CORS_ALLOWED_ORIGINS).
     * "*"로 열어 두면 다른 사이트의 페이지가 사용자 브라우저로 소켓을 열 수 있다. 같은 서버에서 연 페이지(same-origin)와
     * Origin 헤더를 보내지 않는 모바일 앱 클라이언트는 이 목록과 무관하게 허용된다.
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 받을 때 (구독): /sub/chat/room/1
        registry.enableSimpleBroker("/sub");
        // 메시지를 보낼 때 (발행): /pub/chat/message
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-jiburo") // 소켓 연결 엔드포인트
                .setAllowedOriginPatterns(allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));
        // .withSockJS(); // 브라우저 호환성을 위해 필요하다면 추가
    }

    // 인바운드 채널(클라이언트 -> 서버)에 인터셉터 추가
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }
}
