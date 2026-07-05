package com.jiburo.server.domain.finder.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record FinderRouteRecordRequestDto(
        @NotEmpty List<RoutePoint> points
) {
    public record RoutePoint(
            @NotNull Double latitude,
            @NotNull Double longitude,
            @NotNull LocalDateTime recordedAt
    ) {}
}
