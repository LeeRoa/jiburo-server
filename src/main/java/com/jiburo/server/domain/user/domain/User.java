package com.jiburo.server.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment; // DB 코멘트용

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소셜 로그인 제공자 + 식별자 조합 (예: "kakao_12345678")
    // 이 값을 기준으로 기존 회원인지 신규 회원인지 식별합니다.
    @Column(nullable = false, unique = true)
    @Comment("소셜 로그인 식별자 (Provider_ID 형태)")
    private String oauthId;

    // 사용자 닉네임 (화면에 표시되는 이름)
    @Column(nullable = false)
    @Comment("사용자 닉네임")
    private String nickname;

    // 연락처 및 알림용 이메일 (소셜 로그인 정보에서 가져옴)
    @Comment("사용자 이메일")
    private String email;

    // 프로필 이미지 URL (외부 스토리지 또는 소셜 프로필 링크)
    @Comment("프로필 이미지 URL")
    private String profileImageUrl;

    // 현재 달성한 탐정 등급
    // 실제 승급 기준(점수)은 BadgePolicy 테이블에서 관리하지만,
    // 조회 성능을 위해 현재 등급을 유저 엔티티에도 저장
    @Enumerated(EnumType.STRING)
    @Comment("현재 탐정 등급 (BadgeLevel Enum)")
    private BadgeLevel badgeLevel;

    // 누적 활동 점수
    // 이 점수가 BadgePolicy의 기준을 넘으면 등급이 올라간다.
    @Comment("누적 활동 점수")
    private int activityScore;

    // Spring Security 권한
    @Enumerated(EnumType.STRING)
    @Comment("사용자 권한 (USER, ADMIN)")
    private Role role;

    @Builder
    public User(String oauthId, String nickname, String email, String profileImageUrl, Role role) {
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        // 초기 가입 시에는 가장 낮은 등급과 0점으로 시작
        this.badgeLevel = BadgeLevel.BEGINNER_DETECTIVE;
        this.activityScore = 0;
    }

    /**
     * 비즈니스 로직: 활동 점수 증가
     * (참고: 등급 승급 로직은 Service 레이어에서 BadgePolicy를 조회한 후
     * User.updateBadgeLevel() 같은 메소드를 호출하여 처리하는 것이 좋습니다.)
     */
    public void increaseScore(int score) {
        this.activityScore += score;
    }

    /**
     * 비즈니스 로직: 등급 변경
     * Service에서 정책 확인 후 승급 대상일 때 호출
     */
    public void updateBadgeLevel(BadgeLevel newLevel) {
        this.badgeLevel = newLevel;
    }
}