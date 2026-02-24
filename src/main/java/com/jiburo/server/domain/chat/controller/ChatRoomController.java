package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.*;
import com.jiburo.server.domain.chat.service.ChatRoomService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
     * [GET] 채팅 메시지 내역 조회
     * @param roomId Hashids String
     */
    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatRoomDetailDto> getRoomMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal CustomOAuth2User user,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(chatRoomService.findRoomDetail(HashidsUtils.decode(roomId), user.getUserId(), pageable));
    }

    /**
     * [GET] 채팅 메시지 키워드 검색
     */
    @GetMapping("/{roomId}/messages/search")
    public ApiResponse<Slice<ChatMessageResponseDto>> searchMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal CustomOAuth2User user,
            @ModelAttribute ChatMessageSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(chatRoomService.searchChatMessages(HashidsUtils.decode(roomId), condition, user.getUserId(), pageable));
    }

    // TODO 채팅방 삭제
}
