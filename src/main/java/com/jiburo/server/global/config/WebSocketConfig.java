package com.jiburo.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지를 받을 때 (구독): /sub/chat/room/1
        config.enableSimpleBroker("/sub");
        // 메시지를 보낼 때 (발행): /pub/chat/message
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-jiburo") // 소켓 연결 엔드포인트
                .setAllowedOriginPatterns("*"); // 우선 테스트를 위해 전체 허용
        // .withSockJS(); // 브라우저 호환성을 위해 필요하다면 추가
    }
}