package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.detail.TargetDetailDto; // [중요] 상세 객체 임포트
import com.jiburo.server.domain.post.domain.LostPostImage;
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LostPostResponseDto(
        @Schema(description = "게시글 ID (인코딩된 문자열)") String id,
        @Schema(description = "작성자 ID") UUID authorId,
        @Schema(description = "작성자 닉네임") String authorNickname,
        @Schema(description = "게시글 제목") String title,
        @Schema(description = "실종정보에 대한 상세 설명") String content,
        @Schema(description = "게시글 상태") PostStatus statusCode,
        @Schema(description = "대분류 코드") CategoryType categoryCode,
        @Schema(description = "카테고리별 상세 정보 (동물의 경우 AnimalDetailDto)") TargetDetailDto detail,
        @Schema(description = "대표 이미지 URL (imageUrls의 0번째)") String imageUrl,
        @Schema(description = "게시글 이미지 URL 목록 (0번째가 대표 이미지)") List<String> imageUrls, // 등록 순서대로 전체 이미지 (수정 요청의 imageUrls와 동일한 필드명/형식)
        @Schema(description = "실종/발견 위치 위도") Double latitude,
        @Schema(description = "실종/발견 위치 경도") Double longitude,
        @Schema(description = "실종/발견 장소 주소 텍스트") String foundLocation,
        @Schema(description = "실종/발견 날짜") LocalDate lostDate,
        @Schema(description = "사례금 (원 단위)") int reward,
        @Schema(description = "게시글 등록 시각") LocalDateTime createdAt,
        @Schema(description = "현재 로그인한 사용자의 좋아요(관심 등록) 여부") boolean isLiked,
        @Schema(description = "좋아요(관심 등록) 수") long likeCount
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
                entity.getImages().stream().map(LostPostImage::getImageUrl).map(R2UrlProvider::buildUrl).toList(),
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
