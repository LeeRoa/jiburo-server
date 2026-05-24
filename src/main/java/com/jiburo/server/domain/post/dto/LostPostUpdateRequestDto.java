package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.global.error.JiburoException;

import java.time.LocalDate;

import static com.jiburo.server.global.error.ErrorCode.FEATURE_NOT_READY;
import static com.jiburo.server.global.error.ErrorCode.POST_CATEGORY_INVALID;

public record LostPostUpdateRequestDto(
        String title,
        String content,
        String imageUrl,
        PostStatus statusCode,

        CategoryType categoryCode, // 대분류 (ANIMAL, PERSON...)

        // --- 상세 정보 필드들 ---
        AnimalType animalTypeCode,
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
) {

    public TargetDetailDto toDetail() {
        return switch (this.categoryCode) {

            // [CASE 1] 동물일 때
            case ANIMAL -> AnimalDetailDto.builder()
                    .animalType(this.animalTypeCode)
                    .breed(this.breed)
                    .gender(this.genderCode)
                    .color(this.color)
                    .age(this.age)
                    .build();

            // TODO [CASE 2, 3] 물건, 사람 (구현 예정)
            case ITEM, PERSON ->
                    throw new JiburoException(FEATURE_NOT_READY);

            // 정의되지 않은 카테고리가 오면 에러 처리
            default -> throw new JiburoException(POST_CATEGORY_INVALID);
        };
    }
}
