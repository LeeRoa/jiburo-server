package com.jiburo.server.domain.post.dto;

public record LostPostMapRequestDto(
        Double minLat, Double maxLat,
        Double minLng, Double maxLng,
        Integer limit
) {
    public LostPostMapRequestDto {
        if (limit == null) limit = 100; // 지도에는 좀 더 많이 (기본 100개)
    }
}
