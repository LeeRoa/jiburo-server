package com.jiburo.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 알림 읽음 처리 요청
 * 단건 처리는 물론, 리스트를 받아 다중건(Bulk) 처리도 가능
 */
public record NotificationReadRequestDto(
        @Schema(description = "읽음 처리할 알림 ID 목록 (단건/다중건 모두 지원)")
        List<Long> notificationIds
) {
    // 단건 처리를 위한 편의 메서드
    public static NotificationReadRequestDto of(Long id) {
        return new NotificationReadRequestDto(List.of(id));
    }
}
