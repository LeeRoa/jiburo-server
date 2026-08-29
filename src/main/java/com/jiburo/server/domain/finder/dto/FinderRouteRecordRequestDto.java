package com.jiburo.server.domain.finder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record FinderRouteRecordRequestDto(
        @Schema(description = "기록할 GPS 이동 경로 좌표 목록") @NotEmpty List<RoutePoint> points
) {
    public record RoutePoint(
            @Schema(description = "경로 지점 위도") @NotNull Double latitude,
            @Schema(description = "경로 지점 경도") @NotNull Double longitude,
            @Schema(description = "경로 지점 기록 시각") @NotNull LocalDateTime recordedAt
    ) {}
}
