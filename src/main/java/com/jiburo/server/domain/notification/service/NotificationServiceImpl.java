package com.jiburo.server.domain.notification.service;

import com.jiburo.server.domain.notification.dto.NotificationResponseDto;
import com.jiburo.server.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Slice<NotificationResponseDto> getMyNotifications(UUID receiverId, Pageable pageable) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId, pageable)
                .map(NotificationResponseDto::from);
    }

    @Override
    public long getUnreadCount(UUID receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }
}
