package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.chat.domain.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, UUID userId);
}