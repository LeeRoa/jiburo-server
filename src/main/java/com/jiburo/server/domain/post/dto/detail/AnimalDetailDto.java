package com.jiburo.server.domain.post.dto.detail;

import com.jiburo.server.domain.post.domain.enums.AnimalType;
import lombok.Builder;

// type 필드는 JSON 변환 시 자동 포함되지만, 명시적으로 가지고 있어도 됨
public record AnimalDetailDto(
        AnimalType animalType,  // DOG, CAT
        String breed,       // 품종
        String gender,      // MALE, FEMALE
        String color,       // 색상
        Integer age         // 나이
) implements TargetDetailDto {

    @Builder
    public AnimalDetailDto {
    }
}