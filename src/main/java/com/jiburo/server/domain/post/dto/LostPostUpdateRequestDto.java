package com.jiburo.server.domain.post.dto;

import java.time.LocalDate;

public record LostPostUpdateRequestDto(
        String title,
        String content,
        String imageUrl,

        String categoryCode, // 대분류 (ANIMAL, PERSON...)

        // --- 상세 정보 필드들 ---
        String animalTypeCode,
        String breed,
        String genderCode,
        String color,
        Integer age,

        // --- 위치 및 기타 정보 ---
        Double latitude,
        Double longitude,
        String foundLocation,

        LocalDate lostDate,
        int reward
) {}