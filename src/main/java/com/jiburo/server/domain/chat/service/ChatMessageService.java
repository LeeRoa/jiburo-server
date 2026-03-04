package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatReadDto;

import java.util.UUID;

public interface ChatMessageService {
    ChatMessageResponseDto saveMessage(Long roomId, UUID senderId, ChatMessageRequestDto dto);

    ChatReadDto.Response processReadReceipt(UUID userId, Long roomId, Long lastReadMessageId);
}
