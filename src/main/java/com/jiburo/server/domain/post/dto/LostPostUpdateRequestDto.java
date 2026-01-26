package com.jiburo.server.domain.post.dto;

import java.time.LocalDate;

public record LostPostUpdateRequestDto(
        String title,
        String content,
        String imageUrl,

        String animalTypeCode,
        String breed,
        String genderCode,
        String color,
        Integer age,

        Double latitude,
        Double longitude,
        String foundLocation,

        LocalDate lostDate,
        int reward
) {}