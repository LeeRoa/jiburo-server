package com.jiburo.server.domain.post.dto;

public record LostPostNearbyRequestDto(
        Double centerLat,
        Double centerLng,
        Double radius, // km 단위 (예: 3.0)
        Integer limit,
        Integer page, // 페이지 번호 (0부터 시작)
        Integer size  // 한 번에 가져올 개수 (기본 20)
) {
    public LostPostNearbyRequestDto {
        if (radius == null) radius = 3.0; // 기본 반경 3km
        if (limit == null) limit = 20;    // 기본 개수 20개
        if (page == null) page = 0;       // 기본값 0페이지
        if (size == null) size = 20;      // 기본값 20개
    }
}
