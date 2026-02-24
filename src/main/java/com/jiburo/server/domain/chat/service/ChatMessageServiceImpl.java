package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    /**
     * 실시간 채팅 메시지 저장 및 방 정보 갱신
     */
    @Override
    public ChatMessageResponseDto saveMessage(Long roomId, UUID senderId, ChatMessageRequestDto dto) {
        // 1. 방 존재 여부 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new JiburoException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 보낸 사람이 이 방의 참여자가 맞는지 권한 체크
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, senderId)
                .orElseThrow(() -> new JiburoException(ErrorCode.NOT_CHAT_PARTICIPANT));

        // 3. 메시지 엔티티 생성 및 저장 (DTO의 toEntity 활용)
        ChatMessage message = dto.toEntity(room, participant.getUser());
        chatMessageRepository.save(message);

        // 4. 방의 마지막 대화 내용과 시간 최신화 (목록 정렬 및 노출용)
        room.updateLastChat(message.getContent());

        // 5. 응답용 DTO로 변환하여 반환
        return ChatMessageResponseDto.from(message);
    }
}
