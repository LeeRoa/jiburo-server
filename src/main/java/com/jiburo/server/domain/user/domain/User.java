package com.jiburo.server.domain.user.domain;

import com.jiburo.server.global.consts.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false, length = 20)
    @Comment("현재 탐정 등급 코드 (BEGINNER, SENIOR...)")
    private String badgeCode;

    @Comment("누적 활동 점수")
    private int activityScore;

    @Column(nullable = false, length = 20)
    @Comment("사용자 권한 코드 (USER, ADMIN)")
    private String roleCode;

    @Builder
    public User(String oauthId, String nickname, String email, String profileImageUrl, String roleCode) {
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.roleCode = roleCode;

        // 초기 가입 시 기본값 설정 (DB 코드로 하드코딩 or 상수로 관리)
        this.badgeCode = "BEGINNER";
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
    public void updateBadge(String newBadgeCode) {
        this.badgeCode = newBadgeCode;
    }
}