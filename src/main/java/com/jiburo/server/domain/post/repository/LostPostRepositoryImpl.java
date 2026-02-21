package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.LostPostMapRequestDto;
import com.jiburo.server.domain.post.dto.LostPostNearbyRequestDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static com.jiburo.server.domain.post.domain.QLostPost.lostPost;
import static com.jiburo.server.domain.user.domain.QUser.user;

/**
 * 게시글 조회 관련 QueryDSL 구현체
 * - 동적 검색 (JSON 필드 포함)
 * - 지도 기반 조회 (Viewport, Radius)
 * - 무한 스크롤 (Slice)
 */
@Slf4j
@RequiredArgsConstructor
public class LostPostRepositoryImpl implements LostPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt", "lostDate", "reward"
    );

    // [보안/유효성] 검색을 허용할 JSON Key 목록 (화이트리스트)
    private static final Set<String> ALLOWED_JSON_KEYS = Set.of(
            "animalType", "breed", "gender", "color", "age", // 동물 관련
            "clothing", "height", "name",                    // 사람 관련 (확장 대비)
            "brand", "model"                                 // 물건 관련 (확장 대비)
    );

    /**
     * [통합 검색] 조건에 맞는 게시글 페이징 조회
     * - 기본 필드 + JSON 상세 필드 동적 검색 지원
     * - Count 쿼리 최적화 적용 (PageableExecutionUtils)
     */
    @Override
    public Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable) {
        // 1) 검색 조건 생성
        BooleanBuilder predicates = getSearchPredicates(condition);

        // 2) 컨텐츠 조회
        List<LostPost> content = queryFactory
                .selectFrom(lostPost)
                .leftJoin(lostPost.user, user).fetchJoin()
                .where(predicates)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
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

    /**
     * [지도 렌더링] Viewport(화면 영역) 기반 게시글 조회
     * - 지도의 남서(SW) ~ 북동(NE) 좌표 사이의 데이터를 조회
     * - 인덱스(idx_lost_post_location)를 사용하여 빠른 조회 가능
     * - 마커 렌더링 성능을 위해 limit 적용
     */
    @Override
    public List<LostPost> searchByViewport(LostPostMapRequestDto request) {
        return queryFactory
                .selectFrom(lostPost)
                .where(
                        latitudeBetween(request.minLat(), request.maxLat()),
                        longitudeBetween(request.minLng(), request.maxLng())
                )
                .limit(request.limit()) // 마커 너무 많으면 성능 저하되므로 제한
                .fetch();
    }

    /**
     * [리스트 렌더링] 반경(Radius) 기반 무한 스크롤 조회 (Slice)
     * - 중심 좌표 기준 반경 N km 이내 데이터 조회
     * - 성능 최적화 1: Bounding Box로 1차 필터링하여 인덱스 유도
     * - 성능 최적화 2: Haversine 공식으로 정밀 거리 계산 및 정렬
     * - 성능 최적화 3: Slice 방식 (Limit + 1)으로 Count 쿼리 제거
     */
    @Override
    public Slice<LostPost> searchByRadius(LostPostNearbyRequestDto request) {
        // 1. 페이징 정보 생성 (page, size)
        Pageable pageable = PageRequest.of(request.page(), request.size());

        // 2. [최적화] 반경 기반 Bounding Box 생성 (인덱스 태우기용)
        // 위도 1도 ≒ 111km (근사치 적용하여 1차 필터링 범위 설정)
        double radiusInDegree = request.radius() / 111.0;
        Double minLat = request.centerLat() - radiusInDegree;
        Double maxLat = request.centerLat() + radiusInDegree;
        Double minLng = request.centerLng() - radiusInDegree;
        Double maxLng = request.centerLng() + radiusInDegree;

        // 3. Haversine 거리 계산 표현식 생성
        NumberExpression<Double> distanceExpression = getDistanceExpression(request.centerLat(), request.centerLng());

        // 4. 쿼리 실행 (요청 사이즈 + 1 조회)
        List<LostPost> content = queryFactory
                .selectFrom(lostPost)
                .where(
                        latitudeBetween(minLat, maxLat),
                        longitudeBetween(minLng, maxLng),
                        distanceExpression.loe(request.radius())
                )
                .orderBy(distanceExpression.asc()) // 거리순 정렬
                .offset(pageable.getOffset())      // 어디서부터 가져올지 (page * size)
                .limit(pageable.getPageSize() + 1) // 요청보다 1개 더 가져옴 (hasNext 확인용)
                .fetch();

        // 4. hasNext 판단 로직
        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize()); // 확인용으로 가져온 마지막 1개는 제거
            hasNext = true; // 다음 페이지 있음
        }

        // 5. Slice 객체로 반환
        return new SliceImpl<>(content, pageable, hasNext);
    }

    private static final OrderSpecifier<?> DEFAULT_ORDER = lostPost.createdAt.desc();

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        if (sort.isUnsorted()) {
            return new OrderSpecifier[]{DEFAULT_ORDER};
        }

        PathBuilder<LostPost> pathBuilder = new PathBuilder<>(LostPost.class, lostPost.getMetadata().getName());

        OrderSpecifier<?>[] result = sort.stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .map(order -> {
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                    return new OrderSpecifier<>(direction, pathBuilder.getComparable(order.getProperty(), Comparable.class));
                })
                .toArray(OrderSpecifier[]::new);

        return result.length > 0 ? result : new OrderSpecifier[]{DEFAULT_ORDER};
    }

    private BooleanExpression latitudeBetween(Double min, Double max) {
        return min != null && max != null ? lostPost.latitude.between(min, max) : null;
    }

    private BooleanExpression longitudeBetween(Double min, Double max) {
        return min != null && max != null ? lostPost.longitude.between(min, max) : null;
    }

    /**
     * Haversine 공식을 이용한 거리 계산 표현식 (MySQL 호환)
     * - 6371: 지구 반지름 (km)
     * - 결과 단위: km
     */
    private NumberExpression<Double> getDistanceExpression(Double lat, Double lng) {
        return Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({0})) * sin(radians({1})))",
                lat, lostPost.latitude, lostPost.longitude, lng, lat, lostPost.latitude);
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
     * JSON 동적 필터 생성
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
                    Expressions.constant("$." + jsonKey)
            ).eq(jsonValue);

            jsonBuilder.and(expr);
        }

        return jsonBuilder;
    }

    // --- 기본 검색 조건 ---

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
