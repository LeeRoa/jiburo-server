package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.global.util.HashidsUtils;
import org.springframework.data.domain.Slice;

import java.util.List;

public record ChatRoomDetailDto(
        String roomId,
        String postId,
        String postTitle,
        String postStatusCode,
        List<ChatMessageResponseDto> messageHistory,
        boolean hasNext // 다음 페이지 존재 여부
) {
    public static ChatRoomDetailDto from(ChatRoom room, Slice<ChatMessage> messages, Long partnerLastReadId) {
        return new ChatRoomDetailDto(
                HashidsUtils.encode(room.getId()),
                HashidsUtils.encode(room.getPost().getId()),
                room.getPost().getTitle(),
                room.getPost().getStatusCode(),
                messages.stream()
                        .map(msg -> {
                            // 상대방의 커서(partnerLastReadId)보다 메시지 PK가 크면 안 읽음(1), 작거나 같으면 읽음(0)
                            int unreadCount = (msg.getId() > partnerLastReadId) ? 1 : 0;
                            return ChatMessageResponseDto.from(msg, unreadCount);
                        })
                        .toList(),
                messages.hasNext()
        );
    }
}
