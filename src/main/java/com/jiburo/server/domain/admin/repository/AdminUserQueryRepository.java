package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.jiburo.server.domain.admin.domain.QUserAdminRole.userAdminRole;
import static com.jiburo.server.domain.user.domain.QUser.user;

/**
 * 관리자 사용자 목록 검색 (QueryDSL)
 */
@Repository
@RequiredArgsConstructor
public class AdminUserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public record Condition(String keyword, String provider, Boolean adminOnly, Long adminRoleId) {}

    public Page<User> search(Condition condition, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();

        if (StringUtils.hasText(condition.keyword())) {
            String kw = condition.keyword().trim();
            where.and(user.nickname.containsIgnoreCase(kw)
                    .or(user.email.containsIgnoreCase(kw))
                    .or(user.oauthId.eq(kw)));
        }
        if (StringUtils.hasText(condition.provider())) {
            where.and(user.oauthId.startsWith(condition.provider().trim().toLowerCase() + "_"));
        }
        if (Boolean.TRUE.equals(condition.adminOnly())) {
            where.and(user.roleCode.eq(RoleType.ADMIN));
        }
        if (condition.adminRoleId() != null) {
            where.and(JPAExpressions.selectOne().from(userAdminRole)
                    .where(userAdminRole.user.id.eq(user.id), userAdminRole.role.id.eq(condition.adminRoleId()))
                    .exists());
        }

        List<User> content = queryFactory.selectFrom(user)
                .where(where)
                .orderBy(user.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable,
                () -> queryFactory.select(user.count()).from(user).where(where).fetchOne());
    }
}
