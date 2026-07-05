package com.jiburo.server.domain.finder.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FinderRoutePointResponseDto(
        Double latitude,
        Double longitude,
        LocalDateTime recordedAt
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
