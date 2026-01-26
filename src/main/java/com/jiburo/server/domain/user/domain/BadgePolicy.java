package com.jiburo.server.domain.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment; // (선택 사항) DB 테이블에도 코멘트를 남기고 싶다면 사용

@Entity
@Getter
@NoArgsConstructor
@Table(name = "badge_policy")
public class BadgePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    @Comment("뱃지 등급 식별자 (Enum 값)")
    private BadgeLevel badgeLevel;

    @Column(nullable = false)
    @Comment("등급 달성에 필요한 최소 활동 점수")
    private int requiredScore;

    @Column(nullable = false)
    @Comment("뱃지 이름 다국어 메시지 키")
    private String titleKey;

    @Column(nullable = false)
    @Comment("뱃지 설명 다국어 메시지 키")
    private String descriptionKey;
}