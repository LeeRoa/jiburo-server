package com.jiburo.server.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeLevel {
    BEGINNER_DETECTIVE,
    JUNIOR_DETECTIVE,
    SENIOR_DETECTIVE,
    MASTER_SHERLOCK;
}