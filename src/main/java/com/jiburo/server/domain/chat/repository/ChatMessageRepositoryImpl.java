package com.jiburo.server.domain.chat.repository;

import com.jiburo.server.domain.chat.domain.ChatMessage;
import com.jiburo.server.domain.chat.domain.QChatMessage;
import com.jiburo.server.domain.chat.dto.ChatMessageResponseDto;
import com.jiburo.server.domain.chat.dto.ChatMessageSearchCondition;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
                        messageTypeEq(condition.messageTypeCode()),
                        createdAtGoe(condition.fromDate()),
                        createdAtLoe(condition.toDate())
                )
                .orderBy(getOrderSpecifiers(pageable))
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

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        // 1. 프론트엔드에서 정렬 조건을 아예 안 보냈을 경우 기본값(최신순)
        if (pageable.getSort().isEmpty()) {
            return new OrderSpecifier[]{QChatMessage.chatMessage.createdAt.asc()};
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        // 2. Pageable 안에 있는 여러 개의 정렬 조건을 모두 순회
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

            // 3. 필드명에 따라 매핑 (프론트엔드에서 보낸 파라미터 이름 기준)
            switch (order.getProperty()) {
                case "createdAt":
                    orders.add(new OrderSpecifier<>(direction, QChatMessage.chatMessage.createdAt));
                    break;
                 case "id":
                     orders.add(new OrderSpecifier<>(direction, QChatMessage.chatMessage.id));
                     break;
                default:
                    // 정의되지 않은 필드로 정렬을 요청하면 안전하게 생성일 기준으로 덮어씀
                    orders.add(new OrderSpecifier<>(direction, QChatMessage.chatMessage.createdAt));
                    break;
            }
        }

        // List를 가변 인자(Varargs) 배열로 변환해서 리턴
        return orders.toArray(new OrderSpecifier[0]);
    }

    private BooleanExpression keywordContains(String keyword) {
        // 키워드가 없으면 null 반환 -> where 절에서 무시됨 (전체 검색)
        return StringUtils.hasText(keyword) ? QChatMessage.chatMessage.content.contains(keyword) : null;
    }

    private BooleanExpression messageTypeEq(String typeCode) {
        return StringUtils.hasText(typeCode) ? QChatMessage.chatMessage.messageTypeCode.eq(typeCode) : null;
    }

    private BooleanExpression createdAtGoe(String fromDate) {
        if (!StringUtils.hasText(fromDate)) return null;
        // "2026-02-24" 문자열을 LocalDate로 변환 후, 해당 일의 00:00:00으로 설정
        return QChatMessage.chatMessage.createdAt.goe(LocalDate.parse(fromDate).atStartOfDay());
    }

    private BooleanExpression createdAtLoe(String toDate) {
        if (!StringUtils.hasText(toDate)) return null;
        // "2026-02-24" 문자열을 LocalDate로 변환 후, 해당 일의 23:59:59.999999999로 설정
        return QChatMessage.chatMessage.createdAt.loe(LocalDate.parse(toDate).atTime(LocalTime.MAX));
    }
}
