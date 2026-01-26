package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.jiburo.server.domain.post.domain.QLostPost.lostPost;
import static com.jiburo.server.domain.user.domain.QUser.user;

@RequiredArgsConstructor
public class LostPostRepositoryImpl implements LostPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable) {

        // 1. 컨텐츠 조회 (데이터 가져오기)
        List<LostPost> content = queryFactory
                .selectFrom(lostPost)
                .leftJoin(lostPost.user, user).fetchJoin()
                .where(
                        eqStatusCode(condition.statusCode()),
                        eqAnimalType(condition.animalTypeCode()),
                        containsKeyword(condition.keyword()),
                        betweenDate(condition.dateFrom(), condition.dateTo())
                )
                .orderBy(lostPost.createdAt.desc()) // 최신순
                .offset(pageable.getOffset()) // [페이징] 몇 번째부터?
                .limit(pageable.getPageSize()) // [페이징] 몇 개 가져와?
                .fetch();

        // 2. 카운트 쿼리 (전체 개수 세기)
        // fetchCount()는 Deprecated 되었으므로 fetchOne()을 사용
        JPAQuery<Long> countQuery = queryFactory
                .select(lostPost.count())
                .from(lostPost)
                .where(
                        eqStatusCode(condition.statusCode()),
                        eqAnimalType(condition.animalTypeCode()),
                        containsKeyword(condition.keyword()),
                        betweenDate(condition.dateFrom(), condition.dateTo())
                );

        // 3. 최적화된 Page 객체 반환
        // 데이터가 페이지 사이즈보다 적으면 카운트 쿼리를 생략하는 최적화를 해줌
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 동적 쿼리 조건들 (null이면 무시됨) ---

    private BooleanExpression eqStatusCode(String statusCode) {
        return StringUtils.hasText(statusCode) ? lostPost.statusCode.eq(statusCode) : null;
    }

    private BooleanExpression eqAnimalType(String animalTypeCode) {
        return StringUtils.hasText(animalTypeCode) ? lostPost.animalTypeCode.eq(animalTypeCode) : null;
    }

    // 키워드 검색: 제목 OR 내용 OR 발견장소 에 포함되는지 확인
    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return lostPost.title.contains(keyword)
                .or(lostPost.content.contains(keyword))
                .or(lostPost.foundLocation.contains(keyword));
    }

    // 날짜 범위 검색
    private BooleanExpression betweenDate(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;

        if (from != null && to != null) {
            return lostPost.lostDate.between(from, to);
        }
        if (from != null) {
            return lostPost.lostDate.goe(from); // greater or equal
        }
        return lostPost.lostDate.loe(to); // less or equal
    }
}