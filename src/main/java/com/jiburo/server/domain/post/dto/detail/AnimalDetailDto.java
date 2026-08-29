package com.jiburo.server.domain.post.dto.detail;

import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.GenderType;
import lombok.Builder;

// type 필드는 JSON 변환 시 자동 포함되지만, 명시적으로 가지고 있어도 됨
public record AnimalDetailDto(
        String name,             // 반려동물 이름
        AnimalType animalType,   // DOG, CAT, ETC
        String animalTypeCustom, // animalType이 ETC일 때 사용자가 직접 입력한 종 (예: 이구아나)
        String breed,            // 품종
        GenderType gender,       // MALE, FEMALE, UNKNOWN
        String color,            // 색상
        Integer age,             // 나이
        Double weight,           // 몸무게 (kg)
        Boolean isBiting         // 무는지 여부
) implements TargetDetailDto {

    @Builder
    public AnimalDetailDto {
    }
}