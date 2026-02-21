package com.jiburo.server.global.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeConst {

    // 동물 종류 그룹
    public static class Animal {
        public static final String DOG = "DOG";
        public static final String CAT = "CAT";
        public static final String ETC = "ETC";
    }

    // 게시글 상태 그룹
    public static class Status {
        public static final String LOST = "LOST";
        public static final String PROTECTING = "PROTECTING";
        public static final String COMPLETE = "COMPLETE";
    }

    // 뱃지 등급 그룹
    public static class Badge {
        public static final String BEGINNER = "BEGINNER";
        public static final String JUNIOR = "JUNIOR";
        public static final String SENIOR = "SENIOR";
        public static final String MASTER = "MASTER";
    }

    // 권한 그룹
    public static class Role {
        public static final String USER = "USER";
        public static final String ADMIN = "ADMIN";
    }

    // 감사 및 로그 액션 그룹
    public static class LogAction {
        // 게시글 관련
        public static final String POST_CREATE = "POST_CREATE";       // 작성
        public static final String POST_UPDATE = "POST_UPDATE";       // 수정
        public static final String POST_DELETE = "POST_DELETE";       // 삭제
        public static final String POST_STATUS_CHANGE = "POST_STATUS_CHANGE"; // 상태 변경 (실종->완료)

        // 회원 관련
        public static final String USER_JOIN = "USER_JOIN";           // 회원가입
        public static final String USER_LOGIN = "USER_LOGIN";         // 로그인 (옵션)
        public static final String USER_UPDATE = "USER_UPDATE";       // 프로필 수정
        public static final String USER_WITHDRAW = "USER_WITHDRAW";   // 회원 탈퇴

        // 인증/보안 관련
        public static final String AUTH_LOGIN = "AUTH_LOGIN";       // 로그인
        public static final String AUTH_REISSUE = "AUTH_REISSUE";   // 토큰 재발급 (Access Token 갱신)
        public static final String AUTH_LOGOUT = "AUTH_LOGOUT";     // 로그아웃

        // 신고/관리 관련
        public static final String REPORT_CREATE = "REPORT_CREATE";   // 악성 게시글 신고
        public static final String BADGE_UPGRADE = "BADGE_UPGRADE";   // 등급 승급
    }

    // 게시글 대분류 (대상)
    public static class PostCategory {
        public static final String ANIMAL = "ANIMAL"; // 동물
        public static final String PERSON = "PERSON"; // 사람 (추후 확장)
        public static final String ITEM = "ITEM";     // 물건 (추후 확장)
        public static final String ETC = "ETC";     // 기타
    }

    public static class PostVisibility {
        public static final String PUBLIC = "PUBLIC"; // 공개
        public static final String PROTECTED = "PROTECTED"; // 허용된 사람만 공개
        public static final String PRIVATE = "PRIVATE"; // 비공개 (자기 자신만)
    }
}
