package com.jiburo.server.domain.notification.controller;

import com.jiburo.server.domain.notification.dto.NotificationResponseDto;
import com.jiburo.server.domain.notification.service.NotificationService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notification", description = "알람 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    // [내 알림 목록 조회]
    @GetMapping
    public ApiResponse<Slice<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(notificationService.getMyNotifications(user.getUserId(), pageable));
    }

    // [안 읽은 알림 개수 조회]
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal CustomOAuth2User user) {
        return ApiResponse.success(notificationService.getUnreadCount(user.getUserId()));
    }

    // TODO 선택 알림 읽음 처리

    // TODO 전체 알림 읽음 처리 (편의성 기능)

    // TODO 선택 알림 삭제
}
