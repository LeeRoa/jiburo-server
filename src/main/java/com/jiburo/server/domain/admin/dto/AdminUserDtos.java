package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.BadgeType;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record RoleRef(
            @Schema(description = "역할 ID (인코딩된 문자열)") String id,
            @Schema(description = "역할 코드") String code,
            @Schema(description = "역할 이름") String name
    ) {
        public static RoleRef of(AdminRole role) {
            return new RoleRef(HashidsUtils.encode(role.getId()), role.getCode(), role.getName());
        }
    }

    public record UserResponse(
            @Schema(description = "사용자 ID") UUID id,
            @Schema(description = "닉네임") String nickname,
            @Schema(description = "이메일") String email,
            @Schema(description = "가입 경로 (kakao/google/naver)") String provider,
            @Schema(description = "계정 구분 (USER / ADMIN=관리자 역할 보유)") RoleType roleCode,
            @Schema(description = "뱃지") BadgeType badgeCode,
            @Schema(description = "활동 점수") int activityScore,
            @Schema(description = "보유 관리자 역할") List<RoleRef> adminRoles,
            @Schema(description = "가입일") LocalDateTime createdAt,
            @Schema(description = "정지 만료 시각 (null = 정지 아님, 9999-12-31 = 영구)") LocalDateTime suspendedUntil,
            @Schema(description = "정지 사유") String suspendReason
    ) {
        public static UserResponse of(User user, List<AdminRole> roles) {
            String oauthId = user.getOauthId();
            String provider = oauthId != null && oauthId.contains("_") ? oauthId.substring(0, oauthId.indexOf('_')) : null;
            return new UserResponse(user.getId(), user.getNickname(), user.getEmail(), provider, user.getRoleCode(),
                    user.getBadgeCode(), user.getActivityScore(),
                    roles.stream().map(RoleRef::of).toList(), user.getCreatedAt(),
                    user.isSuspended() ? user.getSuspendedUntil() : null, user.isSuspended() ? user.getSuspendReason() : null);
        }
    }

    public record SearchCondition(
            @Schema(description = "닉네임/이메일 부분 일치 또는 소셜 ID(provider_id) 정확히 일치") String keyword,
            @Schema(description = "가입 경로 (kakao/google/naver)") String provider,
            @Schema(description = "true면 관리자 역할 보유 계정만") Boolean adminOnly,
            @Schema(description = "이 역할을 가진 계정만 (역할 ID, 인코딩된 문자열)") String adminRoleId
    ) {}

    public record RolesUpdateRequest(
            @Schema(description = "부여할 역할 ID 목록 (인코딩된 문자열). 이 목록으로 통째로 교체, 빈 목록이면 모두 회수")
            @NotNull @Size(max = 50) List<String> roleIds
    ) {}
}
