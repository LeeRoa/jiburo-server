package com.jiburo.server.domain.notification.domain;

import java.util.UUID;

/** 알림이 저장되면 발행 (Notification의 @DomainEvents) — 푸시 발송 리스너가 커밋 후 받는다 */
public record NotificationCreatedEvent(Long notificationId, UUID receiverId, String typeCode, String args, Long targetId) {}
