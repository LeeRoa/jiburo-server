package com.jiburo.server.domain.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users") // H2 등 일부 DB에서 user가 예약어일 수 있어 users로 지정
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소셜 로그인 식별자 (예: kakao_123456)
    @Column(nullable = false, unique = true)
    private String oauthId;

    @Column(nullable = false)
    private String nickname;

    private String email;

    private String profileImageUrl;

    // 탐정 등급 (게이미피케이션)
    @Enumerated(EnumType.STRING)
    private BadgeLevel badgeLevel;

    // 활동 점수
    private int activityScore;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public User(String oauthId, String nickname, String email, String profileImageUrl, Role role) {
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.badgeLevel = BadgeLevel.BEGINNER_DETECTIVE; // 가입 시 기본 등급
        this.activityScore = 0;
    }

    // 비즈니스 로직: 활동 점수 증가 및 등급 승급
    public void increaseScore(int score) {
        this.activityScore += score;
        // Todo 여기에 점수에 따른 BadgeLevel 승급 로직 추가
    }
}