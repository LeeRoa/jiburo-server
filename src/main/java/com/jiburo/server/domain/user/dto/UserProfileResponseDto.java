package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileResponseDto(
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "이메일") String email,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "로그인 수단 (google, kakao, naver)") String provider,
        @Schema(description = "탐정 등급 코드 (BEGINNER, SENIOR...)") String badgeCode,
        @Schema(description = "누적 활동 점수") int activityScore,
        @Schema(description = "알림 수신 여부") boolean notificationEnabled,
        @Schema(description = "위치 기반 서비스 수신 여부") boolean locationEnabled
) {
    public static UserProfileResponseDto from(User user) {
        return new UserProfileResponseDto(
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                extractProvider(user.getOauthId()),
                user.getBadgeCode().name(),
                user.getActivityScore(),
                user.isNotificationEnabled(),
                user.isLocationEnabled()
        );
    }

    // oauthId는 "{provider}_{providerId}" 형태 — provider 부분만 노출한다
    private static String extractProvider(String oauthId) {
        int separatorIndex = oauthId.indexOf('_');
        return separatorIndex > 0 ? oauthId.substring(0, separatorIndex) : oauthId;
    }
}
