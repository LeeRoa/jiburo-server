package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.user.domain.User;

public record ChatMessageRequestDto(
        String content,
        String messageTypeCode // TALK, IMAGE, MAP 등 (공통코드 참조)
) {
    public ChatMessage toEntity(ChatRoom room, User sender) {
        return ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(this.content)
                .messageTypeCode(this.messageTypeCode)
                .build();
    }
}