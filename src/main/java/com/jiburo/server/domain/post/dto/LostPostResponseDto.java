package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto; // [중요] 상세 객체 임포트
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LostPostResponseDto(
        Long id,
        String authorNickname, // 작성자
        String title,
        String content,
        String statusCode,     // LOST, COMPLETE

        // 기존 개별 필드 삭제 -> 카테고리 & 상세 객체로 통합
        String categoryCode,   // ANIMAL, PERSON, ITEM
        TargetDetailDto detail, // 상세 정보 (JSON 데이터가 객체로 변환된 것)

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

                entity.getCategoryCode(),
                entity.getDetail(),

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