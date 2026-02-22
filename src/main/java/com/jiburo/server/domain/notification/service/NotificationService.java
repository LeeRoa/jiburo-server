package com.jiburo.server.domain.notification.service;

import com.jiburo.server.domain.notification.dto.NotificationResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface NotificationService {
    Slice<NotificationResponseDto> getMyNotifications(UUID receiverId, Pageable pageable);

    long getUnreadCount(UUID receiverId);
}
