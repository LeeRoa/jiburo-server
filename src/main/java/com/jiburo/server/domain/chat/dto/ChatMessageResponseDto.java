package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponseDto(
        String messageId,
        String roomId,
        UUID senderId,
        String senderNickname,
        String content,
        ChatMsgType messageTypeCode,
        LocalDateTime createdAt,
        int unreadCount
) {
    public static ChatMessageResponseDto from(ChatMessage entity, int unreadCount) {
        String content = entity.getContent();
        if (ChatMsgType.IMAGE.equals(entity.getMessageTypeCode()) || ChatMsgType.VIDEO.equals(entity.getMessageTypeCode())) {
            content = R2UrlProvider.buildUrl(content);
        }

        return new ChatMessageResponseDto(
                HashidsUtils.encode(entity.getId()),
                HashidsUtils.encode(entity.getChatRoom().getId()),
                entity.getSender().getId(),
                entity.getSender().getNickname(),
                content,
                entity.getMessageTypeCode(),
                entity.getCreatedAt(),
                unreadCount
        );
    }
}
