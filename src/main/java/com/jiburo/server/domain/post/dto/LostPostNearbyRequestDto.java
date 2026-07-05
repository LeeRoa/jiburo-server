package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.enums.NearbySortType;

public record LostPostNearbyRequestDto(
        Double centerLat,
        Double centerLng,
        Double radius,        // km 단위 (예: 3.0)
        String placeName,     // 장소명 (nullable, 검색 기록 표시용)
        String address,       // 주소 (nullable, 검색 기록 표시용)
        NearbySortType sortType, // 정렬 기준 (DISTANCE: 거리순, LATEST: 최신순)
        Integer limit,
        Integer page,         // 페이지 번호 (0부터 시작)
        Integer size          // 한 번에 가져올 개수 (기본 20)
) {
    public LostPostNearbyRequestDto {
        if (radius == null) radius = 3.0;
        if (limit == null) limit = 20;
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortType == null) sortType = NearbySortType.DISTANCE;
    }
}
