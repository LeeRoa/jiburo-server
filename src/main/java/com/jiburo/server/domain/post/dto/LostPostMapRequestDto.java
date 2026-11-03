package com.jiburo.server.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LostPostMapRequestDto(
        @Schema(description = "뷰포트 최소 위도") Double minLat,
        @Schema(description = "뷰포트 최대 위도") Double maxLat,
        @Schema(description = "뷰포트 최소 경도") Double minLng,
        @Schema(description = "뷰포트 최대 경도") Double maxLng,
        @Schema(description = "최대 조회 개수 (기본 100, 최대 운영 설정 MAP_MAX_MARKERS)") Integer limit
) {
    public LostPostMapRequestDto {
        if (limit == null) limit = 100; // 지도에는 좀 더 많이 (기본 100개)
    }

    public LostPostMapRequestDto withLimit(int effectiveLimit) {
        return new LostPostMapRequestDto(minLat, maxLat, minLng, maxLng, effectiveLimit);
    }
}
