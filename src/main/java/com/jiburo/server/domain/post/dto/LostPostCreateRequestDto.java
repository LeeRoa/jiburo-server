package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.AnimalType;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.GenderType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.JiburoException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

import static com.jiburo.server.global.error.ErrorCode.FEATURE_NOT_READY;
import static com.jiburo.server.global.error.ErrorCode.POST_CATEGORY_INVALID;

public record LostPostCreateRequestDto(
        @NotBlank String title,
        String content, // 내용은 선택

        @NotNull CategoryType categoryCode, // ANIMAL, PERSON
        String name, // 반려동물 이름
        AnimalType animalTypeCode, // DOG, CAT, ETC
        String animalTypeCustom, // animalTypeCode가 ETC일 때 직접 입력한 종 (예: 이구아나)
        String breed,
        GenderType genderCode, // MALE, FEMALE, UNKNOWN
        String color,
        Integer age,
        Double weight,
        Boolean isBiting, // 무는지 여부

        List<String> imageUrls,

        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String foundLocation, // 주소 텍스트

        @NotNull LocalDate lostDate,
        int reward,
        @NotNull VisibilityType visibilityCode
) {
    // 1. 엔티티 변환 메인 메서드
    public LostPost toEntity(User user) {
        return LostPost.builder()
                .user(user)
                .categoryCode(this.categoryCode)
                .title(this.title())
                .content(this.content())
                .statusCode(PostStatus.LOST)
                .detail(this.toDetail()) // 상세정보 생성 로직 위임
                .imageUrl(this.imageUrls() != null && !this.imageUrls().isEmpty() ? this.imageUrls().get(0) : null)
                .latitude(this.latitude())
                .longitude(this.longitude())
                .foundLocation(this.foundLocation())
                .lostDate(this.lostDate())
                .reward(this.reward())
                .visibilityCode(this.visibilityCode())
                .build();
    }

    // 2. 상세 정보 객체 생성 (Private Helper)
    private TargetDetailDto toDetail() {
        return switch (this.categoryCode) {

            // [CASE 1] 동물일 때
            case ANIMAL -> AnimalDetailDto.builder()
                    .name(this.name)
                    .animalType(this.animalTypeCode)
                    .animalTypeCustom(this.animalTypeCustom)
                    .breed(this.breed)
                    .gender(this.genderCode)
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
