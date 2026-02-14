package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.detail.AnimalDetailDto;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.error.BusinessException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

import static com.jiburo.server.global.error.ErrorCode.FEATURE_NOT_READY;
import static com.jiburo.server.global.error.ErrorCode.POST_CATEGORY_INVALID;

public record LostPostCreateRequestDto(
        @NotBlank String title,
        String content, // 내용은 선택

        @NotNull String categoryCode, // ANIMAL, PERSON
        @NotBlank String animalTypeCode, // DOG, CAT
        String breed,
        String genderCode, // MALE, FEMALE
        String color,
        Integer age,

        @NotNull
        @Size(min = 1, message = "최소 1장의 이미지는 필수입니다.")
        List<String> imageUrls,

        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String foundLocation, // 주소 텍스트

        @NotNull LocalDate lostDate,
        int reward
) {
    // 1. 엔티티 변환 메인 메서드
    public LostPost toEntity(User user) {
        return LostPost.builder()
                .user(user)
                .categoryCode(this.categoryCode)
                .title(this.title())
                .content(this.content())
                .statusCode(CodeConst.Status.LOST)
                .detail(this.toDetail()) // 상세정보 생성 로직 위임
                .imageUrl(this.imageUrls().get(0)) // 첫 번째 이미지를 대표 이미지로
                .latitude(this.latitude())
                .longitude(this.longitude())
                .foundLocation(this.foundLocation())
                .lostDate(this.lostDate())
                .reward(this.reward())
                .build();
    }

    // 2. 상세 정보 객체 생성 (Private Helper)
    private TargetDetailDto toDetail() {
        return switch (this.categoryCode) {

            // [CASE 1] 동물일 때
            case CodeConst.PostCategory.ANIMAL -> AnimalDetailDto.builder()
                    .animalType(this.animalTypeCode)
                    .breed(this.breed)
                    .gender(this.genderCode)
                    .color(this.color)
                    .age(this.age)
                    .build();

            // TODO [CASE 2, 3] 물건, 사람 (구현 예정)
            case CodeConst.PostCategory.ITEM, CodeConst.PostCategory.PERSON ->
                    throw new BusinessException(FEATURE_NOT_READY);

            // 정의되지 않은 카테고리가 오면 에러 처리
            default -> throw new BusinessException(POST_CATEGORY_INVALID);
        };
    }
}
