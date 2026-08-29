package com.jiburo.server.domain.finder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record FinderRoutePointResponseDto(
        @Schema(description = "경로 지점 위도") Double latitude,
        @Schema(description = "경로 지점 경도") Double longitude,
        @Schema(description = "경로 지점 기록 시각") LocalDateTime recordedAt
) {
    public static FinderRoutePointResponseDto from(FinderRouteRecordRequestDto.RoutePoint point) {
        return new FinderRoutePointResponseDto(
                point.latitude(),
                point.longitude(),
                point.recordedAt()
        );
    }

    public static List<FinderRoutePointResponseDto> fromList(List<FinderRouteRecordRequestDto.RoutePoint> points) {
        if (points == null) return List.of();
        return points.stream().map(FinderRoutePointResponseDto::from).toList();
    }
}
