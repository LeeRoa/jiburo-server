package com.jiburo.server.domain.post.dto;

import java.time.LocalDate;

// 검색 필터 조건 (null이면 전체 조회)
public record LostPostSearchCondition(
        String statusCode,     // 상태 (LOST, COMPLETE)
        String animalTypeCode, // 동물 종류 (DOG, CAT)
        String keyword,        // 검색어 (제목 + 내용 + 장소)
        LocalDate dateFrom,    // 날짜 검색 시작일
        LocalDate dateTo       // 날짜 검색 종료일
) {}