package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface ChatRoomService {
    ChatRoomListDto createChatRoom(ChatRoomCreateDto dto, UUID hostId);

    List<ChatRoomListDto> findMyChatRooms(UUID userId);

    ChatRoomDetailDto findRoomDetail(Long roomId, UUID userId, Pageable pageable);

    Slice<ChatMessageResponseDto> searchChatMessages(Long roomId, ChatMessageSearchCondition condition, UUID userId, Pageable pageable);
}
