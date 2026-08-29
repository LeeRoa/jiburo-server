package com.jiburo.server.domain.chat.controller;

import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatMessageSearchCondition;
import com.jiburo.server.domain.chat.dto.ChatRoomCreateDto;
import com.jiburo.server.domain.chat.dto.ChatRoomDetailDto;
import com.jiburo.server.domain.chat.dto.ChatRoomListDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ChatRoomControllerDocs {

    @Operation(summary = "채팅방 생성", description = "게시글 기준으로 채팅방을 생성합니다. 이미 상대방과 생성된 채팅방이 있으면 새로 만들지 않고 기존 채팅방 정보를 반환합니다.")
    ApiResponse<ChatRoomListDto> createRoom(
            ChatRoomCreateDto requestDto,
            CustomOAuth2User user
    );

    @Operation(summary = "내 채팅방 목록 조회", description = "로그인한 사용자가 참여 중인 채팅방 목록을 최근 대화순으로 조회합니다.")
    ApiResponse<List<ChatRoomListDto>> getMyRooms(CustomOAuth2User user);

    @Operation(summary = "채팅 메시지 내역 조회", description = "채팅방의 메시지 내역을 페이지 단위로 조회합니다. 게시글 정보와 상대방 읽음 여부가 반영된 메시지 목록을 함께 반환합니다.")
    ApiResponse<ChatRoomDetailDto> getRoomMessages(
            @Parameter(description = "채팅방 ID (인코딩된 문자열)") String roomId,
            CustomOAuth2User user,
            Pageable pageable
    );

    @Operation(summary = "채팅 메시지 검색", description = "채팅방 내 메시지를 키워드, 메시지 타입, 기간 조건으로 검색합니다.")
    ApiResponse<Slice<ChatMessageResponseDto>> searchMessages(
            @Parameter(description = "채팅방 ID (인코딩된 문자열)") String roomId,
            CustomOAuth2User user,
            ChatMessageSearchCondition condition,
            Pageable pageable
    );

    @Operation(summary = "채팅방 나가기", description = "로그인한 사용자를 채팅방에서 퇴장 처리합니다.")
    ApiResponse<Void> exitRoom(
            @Parameter(description = "채팅방 ID (인코딩된 문자열)") String roomId,
            CustomOAuth2User user
    );
}
