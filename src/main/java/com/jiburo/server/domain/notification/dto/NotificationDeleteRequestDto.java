package com.jiburo.server.domain.notification.dto;

import java.util.List;

/**
 * 알림 삭제 요청
 */
public record NotificationDeleteRequestDto(
        List<Long> notificationIds
) {}
