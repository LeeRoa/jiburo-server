package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserSettingsResponseDto(
        @Schema(description = "알림 수신 여부") boolean notificationEnabled,
        @Schema(description = "위치 기반 서비스 수신 여부") boolean locationEnabled
) {
    public static UserSettingsResponseDto from(User user) {
        return new UserSettingsResponseDto(user.isNotificationEnabled(), user.isLocationEnabled());
    }
}
