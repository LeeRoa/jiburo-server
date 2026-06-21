package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto; // [중요] 상세 객체 임포트
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LostPostResponseDto(
        String id,
        UUID authorId,
        String authorNickname,
        String title,
        String content,
        PostStatus statusCode,
        CategoryType categoryCode,
        TargetDetailDto detail,
        String imageUrl,
        Double latitude,
        Double longitude,
        String foundLocation,
        LocalDate lostDate,
        int reward,
        LocalDateTime createdAt,
        boolean isLiked,
        long likeCount
) {
    public static LostPostResponseDto from(LostPost entity, boolean isLiked, long likeCount) {
        return new LostPostResponseDto(
                HashidsUtils.encode(entity.getId()),
                entity.getUser().getId(),
                entity.getUser().getNickname(),
                entity.getTitle(),
                entity.getContent(),
                entity.getStatusCode(),
                entity.getCategoryCode(),
                entity.getDetail(),
                R2UrlProvider.buildUrl(entity.getImageUrl()),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getFoundLocation(),
                entity.getLostDate(),
                entity.getReward(),
                entity.getCreatedAt(),
                isLiked,
                likeCount
        );
    }

    // 지도/반경 마커 조회용 (좋아요 정보 불필요)
    public static LostPostResponseDto from(LostPost entity) {
        return from(entity, false, 0L);
    }
}
