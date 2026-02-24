package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.QChatMessage;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatMessageSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ChatMessageResponseDto> searchMessages(Long roomId, ChatMessageSearchCondition condition, Pageable pageable) {
        QChatMessage message = QChatMessage.chatMessage;

        List<ChatMessage> results = queryFactory
                .selectFrom(message)
                .where(
                        message.chatRoom.id.eq(roomId),
                        keywordContains(condition.keyword()),
                        messageTypeEq(condition.messageTypeCode())
                )
                .orderBy(message.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        List<ChatMessageResponseDto> content = results.stream()
                .limit(pageable.getPageSize())
                .map(ChatMessageResponseDto::from)
                .toList();

        return new SliceImpl<>(content, pageable, results.size() > pageable.getPageSize());
    }

    // --- 동적 쿼리를 위한 BooleanExpression 메서드들 ---

    private BooleanExpression keywordContains(String keyword) {
        // 키워드가 없으면 null 반환 -> where 절에서 무시됨 (전체 검색)
        return StringUtils.hasText(keyword) ? QChatMessage.chatMessage.content.contains(keyword) : null;
    }

    private BooleanExpression messageTypeEq(String typeCode) {
        return StringUtils.hasText(typeCode) ? QChatMessage.chatMessage.messageTypeCode.eq(typeCode) : null;
    }
}
