package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.dto.ChatRoomCreateDto;
import com.jiburo.server.domain.chat.dto.ChatRoomDetailDto;
import com.jiburo.server.domain.chat.dto.ChatRoomListDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChatRoomService {
    ChatRoomListDto createChatRoom(ChatRoomCreateDto dto, UUID hostId);

    List<ChatRoomListDto> findMyChatRooms(UUID userId);

    ChatRoomDetailDto findRoomDetail(Long roomId, UUID userId, Pageable pageable);
}
