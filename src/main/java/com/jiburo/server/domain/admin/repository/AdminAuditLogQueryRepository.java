package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.AuditLog;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.jiburo.server.global.log.QAuditLog.auditLog;

/**
 * 관리자 감사 로그 검색 (QueryDSL, 최신순)
 */
@Repository
@RequiredArgsConstructor
public class AdminAuditLogQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record Condition(UUID userId, LogActionType action, String actionPrefix, LocalDate from, LocalDate to) {}

    public Page<AuditLog> search(Condition condition, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();

        if (condition.userId() != null) {
            where.and(auditLog.userId.eq(condition.userId()));
        }
        if (condition.action() != null) {
            where.and(auditLog.action.eq(condition.action()));
        } else if (StringUtils.hasText(condition.actionPrefix())) {
            // 예: "ADMIN_" → 관리자 작업 전체. enum 이름 기준으로 IN 조건을 만든다 (인덱스 사용 가능)
            String prefix = condition.actionPrefix().trim().toUpperCase();
            List<LogActionType> actions = Arrays.stream(LogActionType.values())
                    .filter(a -> a.name().startsWith(prefix))
                    .toList();
            if (actions.isEmpty()) {
                return Page.empty(pageable);
            }
            where.and(auditLog.action.in(actions));
        }
        if (condition.from() != null) {
            where.and(auditLog.createdAt.goe(condition.from().atStartOfDay()));
        }
        if (condition.to() != null) {
            where.and(auditLog.createdAt.lt(condition.to().plusDays(1).atStartOfDay()));
        }

        List<AuditLog> content = queryFactory.selectFrom(auditLog)
                .where(where)
                .orderBy(auditLog.createdAt.desc(), auditLog.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable,
                () -> queryFactory.select(auditLog.count()).from(auditLog).where(where).fetchOne());
    }
}
