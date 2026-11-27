package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.admin.repository.AdminPostQueryRepository.DeletedFilter;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.domain.LostPostImage;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.domain.enums.VisibilityType;
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminPostDtos {

    private AdminPostDtos() {
    }

    public record SearchCondition(
            @Schema(description = "제목/내용/작성자 닉네임") String keyword,
            @Schema(description = "카테고리") CategoryType category,
            @Schema(description = "상태") PostStatus status,
            @Schema(description = "공개 범위") VisibilityType visibility,
            @Schema(description = "삭제 여부 필터 (EXCLUDE 기본: 삭제 제외 / ONLY: 삭제된 글만 / ALL)") DeletedFilter deleted
    ) {}

    public record PostSummary(
            @Schema(description = "게시글 ID (인코딩된 문자열)") String id,
            @Schema(description = "제목") String title,
            @Schema(description = "작성자 ID") UUID authorId,
            @Schema(description = "작성자 닉네임") String authorNickname,
            @Schema(description = "카테고리") CategoryType category,
            @Schema(description = "상태") PostStatus status,
            @Schema(description = "공개 범위") VisibilityType visibility,
            @Schema(description = "삭제 여부") boolean deleted,
            @Schema(description = "삭제 시각") LocalDateTime deletedAt,
            @Schema(description = "대표 이미지 URL") String imageUrl,
            @Schema(description = "좋아요 수") long likeCount,
            @Schema(description = "채팅방 수") long chatRoomCount,
            @Schema(description = "등록 시각") LocalDateTime createdAt
    ) {
        public static PostSummary of(LostPost p, long likeCount, long chatRoomCount) {
            return new PostSummary(HashidsUtils.encode(p.getId()), p.getTitle(), p.getUser().getId(), p.getUser().getNickname(),
                    p.getCategoryCode(), p.getStatusCode(), p.getVisibilityCode(), p.isDeleted(), p.getDeletedAt(),
                    R2UrlProvider.buildUrl(p.getImageUrl()), likeCount, chatRoomCount, p.getCreatedAt());
        }
    }

    public record PostDetail(
            PostSummary summary,
            @Schema(description = "내용") String content,
            @Schema(description = "전체 이미지 URL") List<String> imageUrls,
            @Schema(description = "발견/실종 장소") String foundLocation,
            @Schema(description = "해결 메모") String resultNote
    ) {
        public static PostDetail of(LostPost p, long likeCount, long chatRoomCount) {
            return new PostDetail(PostSummary.of(p, likeCount, chatRoomCount), p.getContent(),
                    p.getImages().stream().map(LostPostImage::getImageUrl).map(R2UrlProvider::buildUrl).toList(),
                    p.getFoundLocation(), p.getResultNote());
        }
    }

    public record ForceDeleteRequest(
            @Schema(description = "삭제 사유 (감사 로그에 기록, 필수)", example = "허위 게시글 신고 다수")
            @NotBlank @Size(max = 500) String reason
    ) {}
}
