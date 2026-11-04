package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponseDto(
        @Schema(description = "메시지 ID (인코딩된 문자열)") String messageId,
        @Schema(description = "채팅방 ID (인코딩된 문자열)") String roomId,
        @Schema(description = "발신자 사용자 ID (UUID)") UUID senderId,
        @Schema(description = "발신자 닉네임") String senderNickname,
        @Schema(description = "메시지 내용 (이미지/영상/이모티콘 타입인 경우 접근 가능한 URL)") String content,
        @Schema(description = "메시지 타입 코드") ChatMsgType messageTypeCode,
        @Schema(description = "메시지 전송 시각") LocalDateTime createdAt,
        @Schema(description = "읽지 않은 인원 수") int unreadCount
) {
    public static ChatMessageResponseDto from(ChatMessage entity, int unreadCount) {
        String content = entity.getContent();
        ChatMsgType type = entity.getMessageTypeCode();
        if (ChatMsgType.IMAGE.equals(type) || ChatMsgType.VIDEO.equals(type) || ChatMsgType.EMOTICON.equals(type)) {
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
