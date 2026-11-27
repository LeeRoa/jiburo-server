package com.jiburo.server.domain.chat.repository;

import java.util.List;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {
    // 특정 방의 메시지 내역을 최신순(혹은 과거순)으로 페이징 조회할 때 사용
    Slice<ChatMessage> findAllByChatRoomId(Long roomId, Pageable pageable);

    // 읽음 처리 시 해당 방의 메시지인지 확인
    boolean existsByIdAndChatRoomId(Long id, Long chatRoomId);

    // 신고 처리 화면의 앞뒤 대화 (신고된 메시지 기준)
    List<ChatMessage> findTop5ByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long messageId);

    List<ChatMessage> findTop5ByChatRoomIdAndIdGreaterThanOrderByIdAsc(Long roomId, Long messageId);
}
