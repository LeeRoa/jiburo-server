package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserSettingsUpdateRequestDto(
        @Schema(description = "알림 수신 여부 (null이면 유지)") Boolean notificationEnabled,
        @Schema(description = "위치 기반 서비스 수신 여부 (null이면 유지)") Boolean locationEnabled
) {}
