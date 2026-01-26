package com.jiburo.server.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LostPostCreateRequestDto(
        @NotBlank String title,
        String content, // 내용은 선택

        @NotBlank String animalTypeCode, // DOG, CAT
        String breed,
        String genderCode, // MALE, FEMALE
        String color,
        Integer age,

        @NotBlank String imageUrl,

        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String foundLocation, // 주소 텍스트

        @NotNull LocalDate lostDate,
        int reward
) {}