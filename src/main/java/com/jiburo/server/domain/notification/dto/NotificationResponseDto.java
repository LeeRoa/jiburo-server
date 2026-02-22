package com.jiburo.server.domain.notification.dto;

import com.jiburo.server.domain.notification.domain.Notification;
import com.jiburo.server.global.util.HashidsUtils;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 알림 목록 조회 및 실시간 전송을 위한 응답 DTO
 */
public record NotificationResponseDto(
        @Comment("알림 ID")
        String id,

        @Comment("알림 유형 (번역 키)")
        String typeCode,

        @Comment("메시지 치환 인자 리스트")
        List<String> args,

        @Comment("이동 대상 ID")
        Long targetId,

        @Comment("읽음 여부")
        boolean isRead,

        @Comment("알림 생성 시간")
        LocalDateTime createdAt
) {
    public static NotificationResponseDto from(Notification notification) {
        return new NotificationResponseDto(
                HashidsUtils.encode(notification.getId()),
                notification.getTypeCode(),
                // 엔티티의 "A,B" 문자열을 ["A", "B"] 리스트로 변환하여 전달
                notification.getArgs() != null ? Arrays.asList(notification.getArgs().split(",")) : List.of(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
