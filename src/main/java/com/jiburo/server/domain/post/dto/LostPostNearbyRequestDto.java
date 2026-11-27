package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.enums.NearbySortType;
import io.swagger.v3.oas.annotations.media.Schema;

public record LostPostNearbyRequestDto(
        @Schema(description = "중심 위도") Double centerLat,
        @Schema(description = "중심 경도") Double centerLng,
        @Schema(description = "검색 반경 (km 단위). 생략 시 운영 설정 NEARBY_DEFAULT_RADIUS_KM, 최대 NEARBY_MAX_RADIUS_KM", example = "3.0") Double radius,
        @Schema(description = "장소명 (nullable, 검색 기록 표시용)") String placeName,
        @Schema(description = "주소 (nullable, 검색 기록 표시용)") String address,
        @Schema(description = "정렬 기준 (DISTANCE: 거리순, LATEST: 최신순)") NearbySortType sortType,
        @Schema(description = "최대 조회 개수") Integer limit,
        @Schema(description = "페이지 번호 (0부터 시작)") Integer page,
        @Schema(description = "한 번에 가져올 개수 (기본 20, 최대 운영 설정 NEARBY_MAX_PAGE_SIZE)") Integer size
) {
    public LostPostNearbyRequestDto {
        if (limit == null) limit = 20;
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortType == null) sortType = NearbySortType.DISTANCE;
    }

    // 운영 설정을 적용한 반경/페이지 크기로 교체한 요청 (서비스에서 사용)
    public LostPostNearbyRequestDto withRadiusAndSize(double effectiveRadius, int effectiveSize) {
        return new LostPostNearbyRequestDto(centerLat, centerLng, effectiveRadius, placeName, address,
                sortType, limit, page, effectiveSize);
    }
}
