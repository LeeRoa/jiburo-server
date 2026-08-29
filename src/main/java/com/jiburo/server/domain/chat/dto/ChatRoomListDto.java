package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ChatRoomListDto(
        @Schema(description = "채팅방 ID (인코딩된 문자열)") String roomId,
        @Schema(description = "상대방(내가 아닌 참여자) 닉네임") String otherUserNickname,
        @Schema(description = "마지막 메시지 내용") String lastMessage,
        @Schema(description = "마지막 메시지 전송 시각") LocalDateTime lastChatAt,
        @Schema(description = "읽지 않은 메시지 수") Long unreadCount
) {
    public static ChatRoomListDto from(ChatRoom room, String otherNickname, long unreadCount) {
        return new ChatRoomListDto(
                HashidsUtils.encode(room.getId()),
                otherNickname,
                room.getLastMessage(),
                room.getLastChatAt(),
                unreadCount
        );
    }
}