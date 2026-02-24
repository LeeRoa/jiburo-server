package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {
    // 특정 방의 메시지 내역을 최신순(혹은 과거순)으로 페이징 조회할 때 사용
    Slice<ChatMessage> findAllByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);
}
