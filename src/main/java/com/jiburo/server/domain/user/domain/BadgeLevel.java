package com.jiburo.server.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeLevel {

    // 초기 가입 시
    BEGINNER_DETECTIVE("초보 탐정", 0, "이제 막 활동을 시작한 탐정입니다."),

    // 활동 점수 30점 이상 (예: 유기견 제보 3회 등)
    JUNIOR_DETECTIVE("성실한 탐정", 30, "꾸준히 활동하며 신뢰를 쌓아가는 탐정입니다."),

    // 활동 점수 100점 이상 (예: 결정적 제보로 주인 찾기 성공)
    SENIOR_DETECTIVE("베테랑 탐정", 100, "많은 동물들의 귀가를 도운 노련한 탐정입니다."),

    // 활동 점수 300점 이상 (최고 등급)
    MASTER_SHERLOCK("명예 셜록", 300, "전설적인 추리력으로 기적을 만드는 명예 셜록입니다.");

    private final String title;         // 화면에 보여줄 뱃지 이름
    private final int requiredScore;    // 승급에 필요한 활동 점수
    private final String description;   // 뱃지 설명 (마이페이지 등에서 노출)

    /**
     * 점수를 입력받아 해당 점수에 맞는 레벨인지 확인하는 로직 등에 활용 가능
     */
    public static BadgeLevel findLevelByScore(int score) {
        // TODO 내림차순으로 체크하여 가장 높은 등급 반환 (로직 구현 시 필요)
        if (score >= MASTER_SHERLOCK.requiredScore) return MASTER_SHERLOCK;
        if (score >= SENIOR_DETECTIVE.requiredScore) return SENIOR_DETECTIVE;
        if (score >= JUNIOR_DETECTIVE.requiredScore) return JUNIOR_DETECTIVE;
        return BEGINNER_DETECTIVE;
    }
}