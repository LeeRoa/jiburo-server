package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.dto.ChatRoomListDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomRepositoryCustom {
    // 특정 게시글에 대해 나와 상대방이 이미 참여 중인 방 조회
    Optional<ChatRoom> findExistedRoom(Long postId, UUID user1, UUID user2);

    // 내 채팅방 목록 및 마지막 메시지, 안 읽은 개수 조회 (Dsl 활용)
    List<ChatRoomListDto> findMyChatRoomsWithUnreadCount(UUID userId);
}
