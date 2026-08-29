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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

import static com.jiburo.server.global.error.ErrorCode.FEATURE_NOT_READY;
import static com.jiburo.server.global.error.ErrorCode.POST_CATEGORY_INVALID;

public record LostPostCreateRequestDto(
        @Schema(description = "게시글 제목") @NotBlank String title,
        @Schema(description = "실종정보에 대한 상세 설명") String content, // 내용은 선택

        @Schema(description = "대분류 코드") @NotNull CategoryType categoryCode, // ANIMAL, PERSON
        @Schema(description = "반려동물 이름", example = "레이") String name, // 반려동물 이름
        @Schema(description = "실종동물 종류. 강아지/고양이가 아니면 ETC + animalTypeCustom") AnimalType animalTypeCode, // DOG, CAT, ETC
        @Schema(description = "animalTypeCode가 ETC일 때 사용자가 직접 입력한 동물 종류", example = "이구아나") String animalTypeCustom, // animalTypeCode가 ETC일 때 직접 입력한 종 (예: 이구아나)
        @Schema(description = "품종/종 구분", example = "리트리버") String breed,
        @Schema(description = "성별") GenderType genderCode, // MALE, FEMALE, UNKNOWN
        @Schema(description = "털색") String color,
        @Schema(description = "나이") Integer age,
        @Schema(description = "몸무게 (kg)") Double weight,
        @Schema(description = "무는지 여부 (제보자 안전 안내용)") Boolean isBiting, // 무는지 여부

        @Schema(description = "게시글 이미지 URL 목록 (0번째가 대표 이미지)") List<String> imageUrls,

        @Schema(description = "실종/발견 위치 위도") @NotNull Double latitude,
        @Schema(description = "실종/발견 위치 경도") @NotNull Double longitude,
        @Schema(description = "실종/발견 장소 주소 텍스트") @NotBlank String foundLocation, // 주소 텍스트

        @Schema(description = "실종/발견 날짜") @NotNull LocalDate lostDate,
        @Schema(description = "사례금 (원 단위)") int reward,
        @Schema(description = "공개 범위") @NotNull VisibilityType visibilityCode
) {
    @AssertTrue(message = "실종동물을 '기타'로 선택한 경우 종류를 직접 입력해주세요.")
    public boolean isAnimalTypeCustomValid() {
        return animalTypeCode != AnimalType.ETC || (animalTypeCustom != null && !animalTypeCustom.isBlank());
    }

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
