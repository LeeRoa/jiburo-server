package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatReadDto;
import com.jiburo.server.domain.chat.service.ChatMessageService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

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
            @Payload ChatMessageRequestDto requestDto,
            Authentication authentication
    ) {
        CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();
        // DB에 메시지 저장 및 방의 마지막 대화 정보 갱신
        Long decodedRoomId = HashidsUtils.decode(roomId);
        ChatMessageResponseDto responseDto = chatMessageService.saveMessage(decodedRoomId, user.getUserId(), requestDto);
        // 해당 방을 구독(/sub/chat/rooms/{roomId})하고 있는 모든 클라이언트에게 메시지 전송
        // 클라이언트가 보낸 문자열이 아니라 서버가 다시 인코딩한 정규 ID로 채널을 정한다 (구독 채널과 항상 일치)
        messagingTemplate.convertAndSend(roomTopic(decodedRoomId), responseDto);
    }

    @MessageMapping("/chat/rooms/{roomId}/read")
    public void readMessage(
            @DestinationVariable String roomId,
            @Payload ChatReadDto.Request requestDto,
            Authentication authentication
    ) {
        CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();

        // 1. DB에 마지막 읽은 메시지 ID 갱신 및 응답 DTO 생성
        ChatReadDto.Response responseDto = chatMessageService.processReadReceipt(
                user.getUserId(),
                HashidsUtils.decode(roomId),
                HashidsUtils.decode(requestDto.lastReadMessageId())
        );

        // 2. 해당 방의 읽음 채널(/sub/chat/rooms/{roomId}/read)을 구독 중인 클라이언트들에게 전송
        messagingTemplate.convertAndSend(roomTopic(HashidsUtils.decode(roomId)) + "/read", responseDto);
    }

    private static String roomTopic(Long roomId) {
        return "/sub/chat/rooms/" + HashidsUtils.encode(roomId);
    }
}
