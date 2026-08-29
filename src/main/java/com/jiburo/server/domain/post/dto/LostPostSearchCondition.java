package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Map;

// 검색 필터 조건 (null이면 전체 조회)
public record LostPostSearchCondition(
        @Schema(description = "대분류 코드") CategoryType categoryCode, // 어떤 대분류인지 (ANIMAL, PERSON...)
        @Schema(description = "게시글 상태") PostStatus statusCode,
        @Schema(description = "검색 키워드 (제목/내용 등)") String keyword,
        @Schema(description = "검색 시작 날짜 (lostDate 기준)") LocalDate dateFrom,
        @Schema(description = "검색 종료 날짜 (lostDate 기준)") LocalDate dateTo,

        @Schema(description = "카테고리별 상세 필터 (자유 키-값, 예: animalTypeCode=DOG, breed=말티즈)")
        Map<String, String> detailFilters // "animalTypeCode=DOG", "breed=말티즈", "clothing=패딩" 무엇이든 받을 수 있는 맵
) {}