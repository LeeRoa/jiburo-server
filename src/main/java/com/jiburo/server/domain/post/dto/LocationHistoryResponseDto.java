package com.jiburo.server.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LocationHistoryResponseDto(
        @Schema(description = "위도") Double lat,
        @Schema(description = "경도") Double lng,
        @Schema(description = "검색 반경 (km)") Double radius,
        @Schema(description = "장소명 (nullable)") String placeName, // 장소명 (nullable)
        @Schema(description = "주소 (nullable)") String address,   // 주소 (nullable)
        @Schema(description = "삭제 시 사용하는 식별자. \"{lat4dp}_{lng4dp}_{radius1dp}\" 형식 (예: 37.5665_126.9780_3.0)") String key        // 삭제 시 사용하는 식별자 ({lat4dp}_{lng4dp}_{radius1dp})
) {
    public static LocationHistoryResponseDto from(String member, String placeName, String address) {
        String[] parts = member.split("_");
        return new LocationHistoryResponseDto(
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                placeName,
                address,
                member
        );
    }
}
