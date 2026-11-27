package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jiburo.server.domain.chat.domain.QChatRoom.chatRoom;
import static com.jiburo.server.domain.post.domain.QLostPost.lostPost;
import static com.jiburo.server.domain.user.domain.QUser.user;

/**
 * 관리자 게시글 검색 — 일반 API와 달리 공개 범위·삭제 여부와 무관하게 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class AdminPostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public enum DeletedFilter { EXCLUDE, ONLY, ALL }

    public record Condition(String keyword, CategoryType category, PostStatus status,
                            VisibilityType visibility, DeletedFilter deleted) {}

    public Page<LostPost> search(Condition c, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();

        if (StringUtils.hasText(c.keyword())) {
            String kw = c.keyword().trim();
            where.and(lostPost.title.contains(kw)
                    .or(lostPost.content.contains(kw))
                    .or(lostPost.user.nickname.contains(kw)));
        }
        if (c.category() != null) where.and(lostPost.categoryCode.eq(c.category()));
        if (c.status() != null) where.and(lostPost.statusCode.eq(c.status()));
        if (c.visibility() != null) where.and(lostPost.visibilityCode.eq(c.visibility()));

        DeletedFilter deleted = c.deleted() != null ? c.deleted() : DeletedFilter.EXCLUDE;
        if (deleted == DeletedFilter.EXCLUDE) where.and(lostPost.deleted.isFalse());
        if (deleted == DeletedFilter.ONLY) where.and(lostPost.deleted.isTrue());

        List<LostPost> content = queryFactory.selectFrom(lostPost)
                .join(lostPost.user, user).fetchJoin()
                .where(where)
                .orderBy(lostPost.createdAt.desc(), lostPost.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(content, pageable,
                () -> queryFactory.select(lostPost.count()).from(lostPost).join(lostPost.user, user).where(where).fetchOne());
    }

    // 게시글별 채팅방 수 (목록 화면용, 한 번에 집계)
    public Map<Long, Long> countChatRooms(Collection<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        List<Tuple> rows = queryFactory.select(chatRoom.post.id, chatRoom.count())
                .from(chatRoom)
                .where(chatRoom.post.id.in(postIds))
                .groupBy(chatRoom.post.id)
                .fetch();
        return rows.stream().collect(Collectors.toMap(t -> t.get(0, Long.class), t -> t.get(1, Long.class)));
    }
}
