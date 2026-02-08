package com.jiburo.server.domain.post.dto;

public record LostPostNearbyRequestDto(
        Double centerLat,
        Double centerLng,
        Double radius, // km 단위 (예: 3.0)
        Integer limit
) {
    public LostPostNearbyRequestDto {
        if (radius == null) radius = 3.0; // 기본 반경 3km
        if (limit == null) limit = 20;    // 기본 개수 20개
    }
}
