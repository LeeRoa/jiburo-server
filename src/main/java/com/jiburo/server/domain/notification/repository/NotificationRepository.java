package com.jiburo.server.domain.notification.repository;

import com.jiburo.server.domain.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(UUID receiverId);
}