package com.jiburo.server.domain.post.dto;

public record LocationHistoryResponseDto(
        Double lat,
        Double lng,
        Double radius,
        String placeName, // 장소명 (nullable)
        String address,   // 주소 (nullable)
        String key        // 삭제 시 사용하는 식별자 ({lat4dp}_{lng4dp}_{radius1dp})
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
