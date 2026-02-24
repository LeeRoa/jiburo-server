package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, UUID userId);

    boolean existsByChatRoomIdAndUserId(Long roomId, UUID userId);
}
