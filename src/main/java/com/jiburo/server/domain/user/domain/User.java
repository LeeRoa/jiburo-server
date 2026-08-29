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
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    private static final String WITHDRAWN_OAUTH_ID_PREFIX = "withdrawn_";
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

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

    @Column(nullable = false)
    @Comment("알림 수신 여부")
    private boolean notificationEnabled;

    @Column(nullable = false)
    @Comment("위치 기반 서비스 수신 여부")
    private boolean locationEnabled;

    @Column(nullable = false)
    @Comment("회원탈퇴 여부 (soft delete)")
    private boolean isDeleted;

    @Comment("회원탈퇴 시각")
    private LocalDateTime deletedAt;

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
        this.notificationEnabled = true;
        this.locationEnabled = true;
        this.isDeleted = false;
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

    public User updateProfile(String nickname, String profileImageUrl) {
        // 기존 값과 다를 때만 갱신 (Dirty Checking)
        if (!Objects.equals(this.nickname, nickname) || !Objects.equals(this.profileImageUrl, profileImageUrl)) {
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
        }
        return this;
    }

    // 마이페이지 프로필 수정 — null인 필드는 기존 값 유지
    public void editProfile(String nickname, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updateSettings(Boolean notificationEnabled, Boolean locationEnabled) {
        if (notificationEnabled != null) this.notificationEnabled = notificationEnabled;
        if (locationEnabled != null) this.locationEnabled = locationEnabled;
    }

    // 탈퇴 후 같은 소셜 계정으로 재가입할 수 있도록 oauthId(unique)를 비워두고 개인정보를 제거한다.
    // 게시글/채팅 등 연관 데이터의 FK는 유지된다.
    public void withdraw() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.oauthId = WITHDRAWN_OAUTH_ID_PREFIX + UUID.randomUUID();
        this.nickname = WITHDRAWN_NICKNAME;
        this.email = null;
        this.profileImageUrl = null;
    }
}
