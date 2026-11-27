package com.jiburo.server.domain.finder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record FinderRouteRecordRequestDto(
        // 요소 단위 제약(@NotNull, 좌표 범위)이 실제로 검사되도록 @Valid로 내려간다
        @Schema(description = "기록할 GPS 이동 경로 좌표 목록 (1회 최대 500개)") @NotEmpty List<@Valid @NotNull RoutePoint> points
) {
    public record RoutePoint(
            @Schema(description = "경로 지점 위도") @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @Schema(description = "경로 지점 경도") @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @Schema(description = "경로 지점 기록 시각") @NotNull LocalDateTime recordedAt
    ) {}
}
