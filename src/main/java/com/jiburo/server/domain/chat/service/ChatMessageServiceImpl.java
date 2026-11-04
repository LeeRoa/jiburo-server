package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.chat.dto.ChatMessageRequestDto;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatReadDto;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.emoticon.service.EmoticonService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.setting.AppSettingService;
import com.jiburo.server.global.setting.SettingKey;
import com.jiburo.server.global.util.HashidsUtils;
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
    private final AppSettingService appSettingService;
    private final EmoticonService emoticonService;

    /**
     * 실시간 채팅 메시지 저장 및 방 정보 갱신
     */
    @Override
    public ChatMessageResponseDto saveMessage(Long roomId, UUID senderId, ChatMessageRequestDto dto) {
        // 1. 방 존재 여부 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new JiburoException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 보낸 사람이 이 방의 (나가지 않은) 참여자가 맞는지 권한 체크
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, senderId)
                .filter(p -> !p.isExited())
                .orElseThrow(() -> new JiburoException(ErrorCode.NOT_CHAT_PARTICIPANT));

        // 3. 입력 검증 (DB 제약 위반으로 500이 나지 않도록 사전 차단)
        validateMessage(dto);

        // 3-1. 이모티콘은 ID로 받아 이미지 키로 바꿔 저장 (이후 이모티콘이 삭제·비노출돼도 과거 메시지는 유지)
        if (dto.messageTypeCode() == ChatMsgType.EMOTICON) {
            String imageKey = emoticonService.resolveImageKey(HashidsUtils.decode(dto.content()));
            dto = new ChatMessageRequestDto(imageKey, ChatMsgType.EMOTICON);
        }

        // 4. 메시지 엔티티 생성 및 저장 (DTO의 toEntity 활용)
        ChatMessage message = dto.toEntity(room, participant.getUser());
        chatMessageRepository.save(message);

        // 5. 보낸 사람은 자기 메시지를 읽은 것으로 처리 (안 하면 내 메시지가 내 '안 읽음' 수에 집계됨)
        participant.updateLastRead(message.getId());

        // 6. 방의 마지막 대화 내용과 시간 최신화 (목록 정렬 및 노출용)
        room.updateLastChat(message.getMessageTypeCode(), message.getContent());

        int unreadCount = room.getParticipants().size() - 1;

        // 7. 응답용 DTO로 변환하여 반환
        return ChatMessageResponseDto.from(message, unreadCount);
    }

    private void validateMessage(ChatMessageRequestDto dto) {
        if (dto == null || dto.messageTypeCode() == null
                || dto.content() == null || dto.content().isBlank()) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (dto.messageTypeCode() == ChatMsgType.TALK && dto.content().length() > appSettingService.getInt(SettingKey.CHAT_MAX_TEXT_LENGTH)) { // 관리자 설정
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Transactional
    @Override
    public ChatReadDto.Response processReadReceipt(UUID userId, Long chatRoomId, Long messageId) {

        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(p -> !p.isExited())
                .orElseThrow(() -> new JiburoException(ErrorCode.NOT_CHAT_PARTICIPANT));

        // DB 업데이트 (더티 체킹)
        participant.updateLastRead(messageId);

        // 브로드캐스트할 응답 데이터 반환
        return new ChatReadDto.Response(userId, messageId);
    }
}
