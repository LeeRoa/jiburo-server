package com.jiburo.server.domain.user.domain;

import com.jiburo.server.domain.user.domain.enums.BadgeType;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @UuidGenerator
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @Column(nullable = false, unique = true)
    @Comment("소셜 로그인 식별자 (Provider_ID 형태)")
    private String oauthId;

    @Column(nullable = false)
    @Comment("사용자 닉네임")
    private String nickname;

    @Comment("사용자 이메일")
    private String email;

    @Comment("프로필 이미지 URL")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("현재 탐정 등급 코드 (BEGINNER, SENIOR...)")
    private BadgeType badgeCode;

    @Comment("누적 활동 점수")
    private int activityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("사용자 권한 코드 (USER, ADMIN)")
    private RoleType roleCode;

    /** 영구 정지를 나타내는 정지 만료 시각 */
    public static final LocalDateTime PERMANENT_SUSPENSION = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Column(name = "suspended_until")
    @Comment("정지 만료 시각 (null = 정지 아님, 9999-12-31 = 영구 정지)")
    private LocalDateTime suspendedUntil;

    @Column(name = "suspend_reason", length = 500)
    @Comment("정지 사유 (관리자 입력)")
    private String suspendReason;

    @Builder
    public User(String oauthId, String nickname, String email, String profileImageUrl, RoleType roleCode) {
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.roleCode = roleCode;

        // 초기 가입 시 기본값 설정 (DB 코드로 하드코딩 or 상수로 관리)
        this.badgeCode = BadgeType.BEGINNER;
        this.activityScore = 0;
    }

    public String getRoleKey() {
        return "ROLE_" + this.roleCode;
    }

    // --- 비즈니스 로직 ---
    public void increaseScore(int score) {
        this.activityScore += score;
    }

    // 등급 변경 (문자열 코드로 받음)
    public void updateBadge(BadgeType newBadgeCode) {
        this.badgeCode = newBadgeCode;
    }

    /**
     * 계정 구분 변경 (USER / ADMIN).
     * ADMIN = 관리자 역할(admin_roles)을 1개 이상 가진 계정. 세부 권한은 역할의 메뉴 권한으로 결정되며,
     * 이 값은 관리자 역할 부여/회수 시 AdminRoleAssignmentService가 자동으로 맞춘다.
     */
    public void changeRole(RoleType roleCode) {
        this.roleCode = roleCode;
    }

    /**
     * 소셜 로그인 시 프로필 동기화.
     * - 소셜 제공자가 값을 주지 않으면(null/빈 값) 기존 값을 유지한다.
     *   (기존 구현은 profileImageUrl이 null인 사용자가 재로그인하면 NPE 발생)
     * - 값이 같으면 필드가 바뀌지 않으므로 Dirty Checking으로 UPDATE도 나가지 않는다.
     */
    public User updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
        return this;
    }

    public boolean isSuspended() {
        return suspendedUntil != null && suspendedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isPermanentlySuspended() {
        return PERMANENT_SUSPENSION.equals(suspendedUntil);
    }

    public void suspend(LocalDateTime until, String reason) {
        this.suspendedUntil = until;
        this.suspendReason = reason;
    }

    public void unsuspend() {
        this.suspendedUntil = null;
        this.suspendReason = null;
    }
}
