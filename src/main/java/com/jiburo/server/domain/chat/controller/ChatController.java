package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.service.ChatMessageService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
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
    @AuditLog(action = CodeConst.LogAction.CHAT_MESSAGE_SEND)
    @MessageMapping("/chat/rooms/{roomId}")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessageRequestDto requestDto,
            Authentication authentication
    ) {
        CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();
        // DB에 메시지 저장 및 방의 마지막 대화 정보 갱신
        ChatMessageResponseDto responseDto = chatMessageService.saveMessage(HashidsUtils.decode(roomId), user.getUserId(), requestDto);
        // 해당 방을 구독(/sub/chat/rooms/{roomId})하고 있는 모든 클라이언트에게 메시지 전송
        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, responseDto);
    }
}
