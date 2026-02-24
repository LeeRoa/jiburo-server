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
    public static ChatRoomDetailDto from(ChatRoom room, Slice<ChatMessage> messages) {
        return new ChatRoomDetailDto(
                HashidsUtils.encode(room.getId()),
                HashidsUtils.encode(room.getPost().getId()),
                room.getPost().getTitle(),
                room.getPost().getStatusCode(),
                messages.stream()
                        .map(ChatMessageResponseDto::from)
                        .toList(),
                messages.hasNext()
        );
    }
}
