package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.global.util.HashidsUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponseDto(
        String messageId,
        String roomId,
        UUID senderId,
        String senderNickname,
        String content,
        String messageTypeCode,
        LocalDateTime createdAt,
        int unreadCount
) {
    public static ChatMessageResponseDto from(ChatMessage entity, int unreadCount) {
        return new ChatMessageResponseDto(
                HashidsUtils.encode(entity.getId()),
                HashidsUtils.encode(entity.getChatRoom().getId()),
                entity.getSender().getId(),
                entity.getSender().getNickname(),
                entity.getContent(),
                entity.getMessageTypeCode(),
                entity.getCreatedAt(),
                unreadCount
        );
    }
}
