package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, UUID userId);

    boolean existsByChatRoomIdAndUserId(Long roomId, UUID userId);

    // 방을 나가지 않은(활성) 참여자인지 확인 — STOMP 구독/메시지 조회 권한 체크용
    boolean existsByChatRoomIdAndUserIdAndIsExitedFalse(Long roomId, UUID userId);

    List<ChatParticipant> findAllByChatRoomId(Long roomId);
}
