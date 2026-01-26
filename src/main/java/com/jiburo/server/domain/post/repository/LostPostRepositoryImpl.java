package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static com.jiburo.server.domain.post.domain.QLostPost.lostPost;
import static com.jiburo.server.domain.user.domain.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class LostPostRepositoryImpl implements LostPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // [보안/유효성] 검색을 허용할 JSON Key 목록 (화이트리스트)
    private static final Set<String> ALLOWED_JSON_KEYS = Set.of(
            "animalType", "breed", "gender", "color", "age", // 동물 관련
            "clothing", "height", "name",                    // 사람 관련 (확장 대비)
            "brand", "model"                                 // 물건 관련 (확장 대비)
    );

    @Override
    public Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable) {

        // 1) 검색 조건 생성
        BooleanBuilder predicates = getSearchPredicates(condition);

        // 2) 컨텐츠 조회
        List<LostPost> content = queryFactory
                .selectFrom(lostPost)
                .leftJoin(lostPost.user, user).fetchJoin()
                .where(predicates)
                .orderBy(lostPost.createdAt.desc(), lostPost.id.desc()) // tie-breaker
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 3) 카운트 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(lostPost.count())
                .from(lostPost)
                .where(predicates);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanBuilder getSearchPredicates(LostPostSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition == null) {
            return builder; // 조건이 없으면 전체 조회
        }

        builder.and(eqCategoryCode(condition.categoryCode()));
        builder.and(eqStatusCode(condition.statusCode()));
        builder.and(containsKeyword(condition.keyword()));
        builder.and(betweenDate(condition.dateFrom(), condition.dateTo()));
        builder.and(buildJsonFilters(condition.detailFilters())); // 빈 builder 반환 -> 안전

        return builder;
    }

    /**
     * [핵심] JSON 동적 필터 생성
     * - null 대신 "빈 BooleanBuilder"를 반환해 and() 처리 안정성 확보
     * - JSON path는 constant()로 감싸 문자열 리터럴로 바인딩을 보장
     */
    private BooleanBuilder buildJsonFilters(Map<String, String> filters) {
        BooleanBuilder jsonBuilder = new BooleanBuilder();

        if (filters == null || filters.isEmpty()) {
            return jsonBuilder;
        }

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String jsonKey = entry.getKey();
            String jsonValue = entry.getValue();

            // 1) 키/값 유효성 체크
            if (!StringUtils.hasText(jsonKey) || !StringUtils.hasText(jsonValue)) {
                continue;
            }

            // 2) 화이트리스트 체크
            if (!ALLOWED_JSON_KEYS.contains(jsonKey)) {
                log.debug("Ignored invalid search key: {}", jsonKey); // warn 과다 방지
                continue;
            }

            // 3) MySQL JSON 검색 조건
            // JSON_UNQUOTE(JSON_EXTRACT(detail, '$.animalType')) = 'DOG'
            BooleanExpression expr = Expressions.stringTemplate(
                    "JSON_UNQUOTE(JSON_EXTRACT({0}, {1}))",
                    lostPost.detail,
                    Expressions.constant("$." + jsonKey) // ✅ 문자열 리터럴로 바인딩 보장
            ).eq(jsonValue);

            jsonBuilder.and(expr);
        }

        return jsonBuilder;
    }

    // --- 기본 조건들 ---

    private BooleanExpression eqCategoryCode(String categoryCode) {
        return StringUtils.hasText(categoryCode) ? lostPost.categoryCode.eq(categoryCode) : null;
    }

    private BooleanExpression eqStatusCode(String statusCode) {
        return StringUtils.hasText(statusCode) ? lostPost.statusCode.eq(statusCode) : null;
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return lostPost.title.contains(keyword)
                .or(lostPost.content.contains(keyword))
                .or(lostPost.foundLocation.contains(keyword));
    }

    private BooleanExpression betweenDate(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;

        if (from != null && to != null) {
            return lostPost.lostDate.between(from, to);
        }
        if (from != null) {
            return lostPost.lostDate.goe(from);
        }
        return lostPost.lostDate.loe(to);
    }
}
