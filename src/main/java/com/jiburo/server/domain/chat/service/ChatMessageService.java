package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;

import java.util.UUID;

public interface ChatMessageService {
    ChatMessageResponseDto saveMessage(Long roomId, UUID senderId, ChatMessageRequestDto dto);
}
