package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.chat.domain.QChatMessage;
import com.jiburo.server.domain.chat.domain.QChatParticipant;
import com.jiburo.server.domain.chat.domain.QChatRoom;
import com.jiburo.server.domain.chat.dto.ChatRoomListDto;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ChatRoom> findExistedRoom(Long postId, UUID user1, UUID user2) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QChatParticipant participant = QChatParticipant.chatParticipant;

        return Optional.ofNullable(
                queryFactory.selectFrom(chatRoom)
                        .join(chatRoom.participants, participant)
                        .where(
                                chatRoom.post.id.eq(postId),
                                participant.user.id.in(user1, user2)
                        )
                        .groupBy(chatRoom.id)
                        .having(participant.count().eq(2L)) // 두 유저가 모두 포함된 방
                        .fetchOne()
        );
    }

    @Override
    public List<ChatRoomListDto> findMyChatRoomsWithUnreadCount(UUID userId) {
        QChatRoom chatRoom = QChatRoom.chatRoom;
        QChatParticipant myPart = QChatParticipant.chatParticipant;
        QChatParticipant otherPart = new QChatParticipant("otherPart");
        QChatMessage message = QChatMessage.chatMessage;

        List<Tuple> results = queryFactory
                .select(
                        chatRoom,
                        otherPart.user.nickname,
                        // 서브쿼리: 내 마지막 읽은 시점 이후의 메시지 개수
                        JPAExpressions.select(message.count())
                                .from(message)
                                .where(message.chatRoom.id.eq(chatRoom.id)
                                        .and(message.createdAt.gt(myPart.lastReadAt)))
                )
                .from(chatRoom)
                .join(chatRoom.participants, myPart)
                .join(chatRoom.participants, otherPart)
                .where(
                        myPart.user.id.eq(userId),
                        myPart.isExited.isFalse(),
                        otherPart.user.id.ne(userId)
                )
                .orderBy(chatRoom.lastChatAt.desc())
                .fetch();

        return results.stream().map(tuple -> {
            ChatRoom room = tuple.get(chatRoom);
            String otherNickname = tuple.get(otherPart.user.nickname);

            // NPE 방어 로직: 널 체크 후 기본값 0L 할당
            Long unreadCountRaw = tuple.get(2, Long.class);
            long unreadCount = (unreadCountRaw != null) ? unreadCountRaw : 0L;

            // DTO의 from 메서드로 넘기기 (Hashids 처리 포함)
            return ChatRoomListDto.from(Objects.requireNonNull(room), otherNickname, unreadCount);
        }).toList();
    }
}
