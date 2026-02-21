package com.jiburo.server.domain.notification.dto;

import com.jiburo.server.domain.notification.domain.Notification;
import java.time.LocalDateTime;

/**
 * 프론트엔드 전달용 알림 데이터
 */
public record NotificationResponseDto(
        Long id,
        String typeCode,      // CHAT, COMMENT, WALK_AREA
        Long targetId,        // 상세 페이지 이동용 ID (게시글 또는 채팅창, 프로필 등..)
        String senderNickname,
        String senderProfile,
        LocalDateTime createdAt
) {
    // 엔티티를 레코드로 변환하는 정적 팩토리 메서드
    public static NotificationResponseDto fromEntity(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getTypeCode(),
                notification.getTargetId(),
                notification.getSender() != null ? notification.getSender().getNickname() : "시스템",
                notification.getSender() != null ? notification.getSender().getProfileImageUrl() : null,
                notification.getCreatedAt()
        );
    }
}
