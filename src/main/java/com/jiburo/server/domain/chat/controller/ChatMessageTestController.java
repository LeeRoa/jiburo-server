package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.service.ChatMessageService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatMessageTestController {

    private final ChatMessageService chatMessageService;

    /**
     * [임시] 메시지 쌓기 테스트용 API
     * Swagger나 Postman에서 HTTP POST로 쉽게 메시지를 누적할 수 있습니다.
     */
    @PostMapping("/{roomId}/messages/test")
    public ApiResponse<ChatMessageResponseDto> saveTestMessage(
            @PathVariable String roomId,
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody ChatMessageRequestDto requestDto) {

        // 서비스 로직 호출 (실제 WebSocket 핸들러에서 호출할 로직과 동일)
        ChatMessageResponseDto response = chatMessageService.saveMessage(HashidsUtils.decode(roomId), user.getUserId(), requestDto);

        return ApiResponse.success(response);
    }
}
