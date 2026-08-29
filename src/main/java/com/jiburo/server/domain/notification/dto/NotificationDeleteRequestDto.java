package com.jiburo.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 알림 삭제 요청
 */
public record NotificationDeleteRequestDto(
        @Schema(description = "삭제할 알림 ID 목록")
        List<Long> notificationIds
) {}
