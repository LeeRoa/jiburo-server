package com.jiburo.server.domain.notification.controller;

import com.jiburo.server.domain.notification.dto.NotificationResponseDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface NotificationControllerDocs {

    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림 목록을 최신순으로 페이징 조회합니다.")
    ApiResponse<Slice<NotificationResponseDto>> getNotifications(CustomOAuth2User user, Pageable pageable);

    @Operation(summary = "안 읽은 알림 개수 조회", description = "로그인한 사용자의 읽지 않은 알림 개수를 조회합니다.")
    ApiResponse<Long> getUnreadCount(CustomOAuth2User user);
}
