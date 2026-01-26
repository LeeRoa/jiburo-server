package com.jiburo.server.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeLevel {

    BEGINNER_DETECTIVE("badge.beginner_detective.title", 0, "badge.beginner_detective.description"),
    JUNIOR_DETECTIVE("badge.junior_detective.title", 30, "badge.junior_detective.description"),
    SENIOR_DETECTIVE("badge.senior_detective.title", 100, "badge.senior_detective.description"),
    MASTER_SHERLOCK("badge.master_sherlock.title", 300, "badge.master_sherlock.description");

    private final String titleKey;
    private final int requiredScore;
    private final String descriptionKey;

    public static BadgeLevel findLevelByScore(int score) {
        if (score >= MASTER_SHERLOCK.requiredScore) return MASTER_SHERLOCK;
        if (score >= SENIOR_DETECTIVE.requiredScore) return SENIOR_DETECTIVE;
        if (score >= JUNIOR_DETECTIVE.requiredScore) return JUNIOR_DETECTIVE;
        return BEGINNER_DETECTIVE;
    }
}