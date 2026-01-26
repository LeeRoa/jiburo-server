package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.LostPost;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LostPostResponseDto(
        Long id,
        String authorNickname, // 작성자
        String title,
        String content,
        String statusCode,     // LOST, COMPLETE

        String animalTypeCode,
        String breed,
        String genderCode,
        String color,
        Integer age,

        String imageUrl,

        Double latitude,
        Double longitude,
        String foundLocation,

        LocalDate lostDate,
        int reward,
        LocalDateTime createdAt
) {
    public static LostPostResponseDto from(LostPost entity) {
        return new LostPostResponseDto(
                entity.getId(),
                entity.getUser().getNickname(),
                entity.getTitle(),
                entity.getContent(),
                entity.getStatusCode(),
                entity.getAnimalTypeCode(),
                entity.getBreed(),
                entity.getGenderCode(),
                entity.getColor(),
                entity.getAge(),
                entity.getImageUrl(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getFoundLocation(),
                entity.getLostDate(),
                entity.getReward(),
                entity.getCreatedAt()
        );
    }
}