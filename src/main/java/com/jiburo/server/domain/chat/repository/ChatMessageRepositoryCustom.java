package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatMessageSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ChatMessageRepositoryCustom {
    Slice<ChatMessageResponseDto> searchMessages(Long roomId, ChatMessageSearchCondition condition, Pageable pageable);
}
