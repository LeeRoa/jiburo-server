package com.jiburo.server.domain.post.dto;

import java.time.LocalDate;
import java.util.Map;

// 검색 필터 조건 (null이면 전체 조회)
public record LostPostSearchCondition(
        String categoryCode, // 어떤 대분류인지 (ANIMAL, PERSON...)
        String statusCode,
        String keyword,
        LocalDate dateFrom,
        LocalDate dateTo,

        // "animalType=DOG", "breed=말티즈", "clothing=패딩" 무엇이든 받을 수 있는 맵
        Map<String, String> detailFilters
) {}