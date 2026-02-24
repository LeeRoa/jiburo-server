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
import com.jiburo.server.global.error.BusinessException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // 채팅 요청자
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 게시글 작성자
        User guest = post.getUser();

        // 본인 게시글에 채팅을 거는 경우 방지
        if (hostId.equals(guest.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_CHAT_WITH_SELF);
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
    public ChatRoomDetailDto findRoomDetail(Long roomId, UUID userId, Pageable pageable) {
        // 권한 체크: 해당 유저가 이 채팅방의 참여자인지 확인
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT));

        // 메시지 내역 조회 (최신순 50개)
        Slice<ChatMessage> messages = chatMessageRepository.findAllByChatRoomId(roomId, pageable);

        // 읽음 처리 (방에 들어왔으니 lastReadAt 최신화)
        participant.updateLastRead();

        return ChatRoomDetailDto.from(participant.getChatRoom(), messages);
    }

    @Override
    public Slice<ChatMessageResponseDto> searchChatMessages(Long roomId, ChatMessageSearchCondition condition, UUID userId, Pageable pageable) {
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT);
        }

        return chatMessageRepository.searchMessages(roomId, condition, pageable);
    }
}
