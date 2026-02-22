package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.global.util.HashidsUtils;

import java.time.LocalDateTime;

public record ChatRoomListDto(
        String roomId,
        String otherUserNickname, // 내가 아닌 상대방 이름
        String lastMessage,
        LocalDateTime lastChatAt,
        Long unreadCount          // ChatParticipant의 lastReadAt으로 계산된 값
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