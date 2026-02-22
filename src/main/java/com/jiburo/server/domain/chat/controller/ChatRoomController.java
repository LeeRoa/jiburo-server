package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.ChatRoomCreateDto;
import com.jiburo.server.domain.chat.dto.ChatRoomDetailDto;
import com.jiburo.server.domain.chat.dto.ChatRoomListDto;
import com.jiburo.server.domain.chat.service.ChatRoomService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 관련 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * [POST] 채팅방 생성
     * @param requestDto postId (Hashids String) 포함
     */
    @PostMapping
    public ApiResponse<ChatRoomListDto> createRoom(
            @RequestBody ChatRoomCreateDto requestDto,
            @AuthenticationPrincipal CustomOAuth2User user) { // Security에서 UUID 기반 유저 정보 획득

        return ApiResponse.success(chatRoomService.createChatRoom(requestDto, user.getUserId()));
    }

    /**
     * [GET] 내 채팅방 목록 조회
     */
    @GetMapping
    public ApiResponse<List<ChatRoomListDto>> getMyRooms(
            @AuthenticationPrincipal CustomOAuth2User user) {

        return ApiResponse.success(chatRoomService.findMyChatRooms(user.getUserId()));
    }

    /**
     * TODO [GET] 채팅 메시지 내역 조회
     * @param roomId Hashids String
     */
    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatRoomDetailDto> getRoomMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal CustomOAuth2User user) {
        return null;
    }

    // TODO 채팅방 삭제
}