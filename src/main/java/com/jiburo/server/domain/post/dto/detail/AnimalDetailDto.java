package com.jiburo.server.domain.post.dto.detail;

import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.GenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// type 필드는 JSON 변환 시 자동 포함되지만, 명시적으로 가지고 있어도 됨
// 필드명은 LostPostCreateRequestDto/LostPostUpdateRequestDto의 요청 필드명과 동일하게 맞춘다
// (프론트가 조회 응답을 그대로 재사용해서 등록/수정 요청을 만들 수 있도록)
public record AnimalDetailDto(
        @Schema(description = "반려동물 이름", example = "레이") String name,
        @Schema(description = "실종동물 종류. 강아지/고양이가 아니면 ETC + animalTypeCustom") AnimalType animalTypeCode,
        @Schema(description = "animalTypeCode가 ETC일 때 사용자가 직접 입력한 동물 종류", example = "이구아나") String animalTypeCustom,
        @Schema(description = "품종/종 구분", example = "리트리버") String breed,
        @Schema(description = "성별") GenderType genderCode,
        @Schema(description = "털색") String color,
        @Schema(description = "나이") Integer age,
        @Schema(description = "몸무게 (kg)") Double weight,
        @Schema(description = "무는지 여부 (제보자 안전 안내용)") Boolean isBiting
) implements TargetDetailDto {

    @Builder
    public AnimalDetailDto {
    }
}