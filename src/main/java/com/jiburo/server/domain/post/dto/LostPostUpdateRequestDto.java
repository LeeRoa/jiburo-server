package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.GenderType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.global.error.JiburoException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

import static com.jiburo.server.global.error.ErrorCode.FEATURE_NOT_READY;
import static com.jiburo.server.global.error.ErrorCode.POST_CATEGORY_INVALID;

public record LostPostUpdateRequestDto(
        @Schema(description = "게시글 제목") String title,
        @Schema(description = "실종정보에 대한 상세 설명") String content,
        @Schema(description = "게시글 이미지 URL 목록 (0번째가 대표 이미지). null이면 이미지 유지, 빈 리스트면 전체 삭제") List<String> imageUrls, // null이면 이미지 유지, 빈 리스트면 전체 삭제 (LostPostCreateRequestDto와 동일한 규칙)
        @Schema(description = "게시글 상태") PostStatus statusCode,

        @Schema(description = "대분류 코드") CategoryType categoryCode, // 대분류 (ANIMAL, PERSON...)

        // --- 상세 정보 필드들 ---
        @Schema(description = "반려동물 이름", example = "레이") String name, // 반려동물 이름
        @Schema(description = "실종동물 종류. 강아지/고양이가 아니면 ETC + animalTypeCustom") AnimalType animalTypeCode,
        @Schema(description = "animalTypeCode가 ETC일 때 사용자가 직접 입력한 동물 종류", example = "이구아나") String animalTypeCustom, // animalTypeCode가 ETC일 때 직접 입력한 종
        @Schema(description = "품종/종 구분", example = "리트리버") String breed,
        @Schema(description = "성별") GenderType genderCode,
        @Schema(description = "털색") String color,
        @Schema(description = "나이") Integer age,
        @Schema(description = "몸무게 (kg)") Double weight,
        @Schema(description = "무는지 여부 (제보자 안전 안내용)") Boolean isBiting, // 무는지 여부

        // --- 위치 및 기타 정보 ---
        @Schema(description = "실종/발견 위치 위도") Double latitude,
        @Schema(description = "실종/발견 위치 경도") Double longitude,
        @Schema(description = "실종/발견 장소 주소 텍스트") String foundLocation,

        @Schema(description = "실종/발견 날짜") LocalDate lostDate,
        @Schema(description = "사례금 (원 단위). 보내지 않으면(null) 기존 값 유지, 0을 보내면 0으로 변경") @PositiveOrZero Integer reward
) {

    public TargetDetailDto toDetail() {
        return switch (this.categoryCode) {

            // [CASE 1] 동물일 때
            case ANIMAL -> AnimalDetailDto.builder()
                    .name(this.name)
                    .animalTypeCode(this.animalTypeCode)
                    .animalTypeCustom(this.animalTypeCustom)
                    .breed(this.breed)
                    .genderCode(this.genderCode)
                    .color(this.color)
                    .age(this.age)
                    .weight(this.weight)
                    .isBiting(this.isBiting)
                    .build();

            // TODO [CASE 2, 3] 물건, 사람 (구현 예정)
            case ITEM, PERSON ->
                    throw new JiburoException(FEATURE_NOT_READY);

            // 정의되지 않은 카테고리가 오면 에러 처리
            default -> throw new JiburoException(POST_CATEGORY_INVALID);
        };
    }
}
