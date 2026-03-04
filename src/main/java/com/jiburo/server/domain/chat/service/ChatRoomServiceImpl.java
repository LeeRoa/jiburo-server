package com.jiburo.server.domain.chat.service;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.ChatParticipant;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.dto.*;
import com.jiburo.server.domain.chat.repository.ChatMessageRepository;
import com.jiburo.server.domain.chat.repository.ChatParticipantRepository;
import com.jiburo.server.domain.chat.repository.ChatRoomRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.repository.LostPostRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LostPostRepository lostPostRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * [채팅방 생성]
     * 1. Hashids로 넘어온 postId를 디코딩하여 게시글 확인
     * 2. 이미 해당 게시글에 대해 나와 상대방의 채팅방이 있는지 조회 (QueryDSL)
     * 3. 없다면 새 방을 생성하고 참여자(나, 상대방)를 등록
     */
    @Override
    @Transactional
    public ChatRoomListDto createChatRoom(ChatRoomCreateDto dto, UUID hostId) {
        // postId 디코딩
        Long postId = dto.getDecodedPostId();

        // id 기반 게시글 조회
        LostPost post = lostPostRepository.findById(postId)
                .orElseThrow(() -> new JiburoException(ErrorCode.POST_NOT_FOUND));

        // 채팅 요청자
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
        // 게시글 작성자
        User guest = post.getUser();

        // 본인 게시글에 채팅을 거는 경우 방지
        if (hostId.equals(guest.getId())) {
            throw new JiburoException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        // 기존 방 존재 여부 확인
        return chatRoomRepository.findExistedRoom(postId, hostId, guest.getId())
                .map(room -> ChatRoomListDto.from(room, guest.getNickname(), 0L))
                .orElseGet(() -> {
                    // 새 방 생성 및 참여자 등록
                    ChatRoom newRoom = chatRoomRepository.save(dto.toEntity(post, host));

                    // 참여자 객체 직접 생성 (방장 + 게시글 작성자)
                    chatParticipantRepository.save(new ChatParticipant(newRoom, host));
                    chatParticipantRepository.save(new ChatParticipant(newRoom, guest));

                    return ChatRoomListDto.from(newRoom, guest.getNickname(), 0L);
                });
    }

    /**
     * [내 채팅방 목록 조회]
     * QueryDSL을 통해 안 읽은 메시지 수와 상대방 정보를 한 번에 긁어옵니다.
     */
    @Override
    public List<ChatRoomListDto> findMyChatRooms(UUID userId) {
        return chatRoomRepository.findMyChatRoomsWithUnreadCount(userId);
    }

    @Override
    @Transactional
    public ChatRoomDetailDto findRoomDetail(Long roomId, UUID userId, Pageable pageable) {
        // 1. 참여자 목록 한 번에 가져오기
        List<ChatParticipant> participants = chatParticipantRepository.findAllByChatRoomId(roomId);

        // 2. 내 정보와 상대방 정보 추출 (한 번의 순회로 최적화 가능하지만, 2명이라 가독성 위주로 작성)
        ChatParticipant myPart = participants.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new JiburoException(ErrorCode.NOT_CHAT_PARTICIPANT));

        ChatParticipant partnerPart = participants.stream()
                .filter(p -> !p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new JiburoException(ErrorCode.PARTNER_NOT_FOUND));

        // 3. 메시지 내역 조회 (Slice 객체)
        Slice<ChatMessage> messages = chatMessageRepository.findAllByChatRoomId(roomId, pageable);

        // 4. 읽음 커서 갱신 및 상대방에게 실시간 알림 발송
        if (messages.hasContent()) {
            Long latestMessageId = messages.getContent().get(0).getId(); // 최신 메시지 ID (DESC 정렬 기준)

            // 내 커서 업데이트
            myPart.updateLastRead(latestMessageId);

            // 상대방 화면의 '안 읽음'을 '읽음'으로 바꾸기 위한 실시간 전송
            // 상대방은 이 메시지를 받고 자신의 화면에 떠 있는 내 메시지들의 상태를 바꿉니다.
            ChatReadDto.Response readReceipt = new ChatReadDto.Response(userId, latestMessageId);
            messagingTemplate.convertAndSend("/sub/chat/rooms/" + HashidsUtils.encode(roomId) + "/read", readReceipt);
        }

        // 5. DTO 반환 (내 메시지 옆에 '읽음/안 읽음'을 표시하기 위해 상대방의 커서 위치를 넘김)
        return ChatRoomDetailDto.from(myPart.getChatRoom(), messages, partnerPart.getLastReadMessageId());
    }

    @Override
    public Slice<ChatMessageResponseDto> searchChatMessages(Long roomId, ChatMessageSearchCondition condition, UUID userId, Pageable pageable) {
        // 1. 권한 체크 (참여자가 아니면 예외 발생)
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new JiburoException(ErrorCode.NOT_CHAT_PARTICIPANT);
        }

        // 2. 상대방의 마지막 읽은 메시지 ID(커서) 조회
        Long partnerLastReadId = chatParticipantRepository.findAllByChatRoomId(roomId).stream()
                .filter(p -> !p.getUser().getId().equals(userId)) // 내가 아닌 유저 필터링
                .findFirst()
                .map(ChatParticipant::getLastReadMessageId)
                .orElseThrow(() -> new JiburoException(ErrorCode.PARTNER_NOT_FOUND));

        // 3. 상대방 커서를 포함하여 검색 쿼리 실행
        return chatMessageRepository.searchMessages(roomId, partnerLastReadId, condition, pageable);
    }
}
