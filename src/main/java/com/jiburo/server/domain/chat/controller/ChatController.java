package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.service.ChatMessageService;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 메시지를 보낼 때 호출되는 핸들러
     * 프론트엔드 요청 경로: /pub/chat/rooms/{roomId}
     */
    @MessageMapping("/chat/rooms/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageRequestDto requestDto
            // 주의: WebSocket 핸들러는 HTTP 세션(Principal)을 바로 가져오기 까다롭습니다.
            // 일단은 테스트를 위해 senderId를 DTO 안에 넣거나, 하드코딩해서 넘긴 뒤 나중에 JWT 인터셉터를 붙일 겁니다.
    ) {
        log.info("채팅 메시지 수신 - 방: {}, 내용: {}", roomId, requestDto.content());

        // 임시로 테스트용 User UUID를 사용합니다. (Jiburo 도메인에 존재하는 실제 유저 ID로 변경 필요)
        UUID senderId = UUID.fromString("f9b91223-b585-498c-984c-4f6400b54946");

        // 1. DB에 메시지 저장 및 방의 마지막 대화 정보 갱신 (아까 만든 서비스 재활용!)
        ChatMessageResponseDto responseDto = chatMessageService.saveMessage(HashidsUtils.decode(roomId), senderId, requestDto);

        // 2. 해당 방을 구독(/sub/chat/rooms/{roomId})하고 있는 모든 클라이언트에게 메시지 쏘기!
        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, responseDto);
    }
}
