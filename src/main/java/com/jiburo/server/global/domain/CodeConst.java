package com.jiburo.server.global.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeConst {

    // 1. 동물 종류 그룹
    public static class Animal {
        public static final String DOG = "DOG";
        public static final String CAT = "CAT";
        public static final String ETC = "ETC";
    }

    // 2. 게시글 상태 그룹
    public static class Status {
        public static final String LOST = "LOST";
        public static final String PROTECTING = "PROTECTING";
        public static final String COMPLETE = "COMPLETE";
    }

    // 3. 뱃지 등급 그룹
    public static class Badge {
        public static final String BEGINNER = "BEGINNER";
        public static final String JUNIOR = "JUNIOR";
        public static final String SENIOR = "SENIOR";
        public static final String MASTER = "MASTER";
    }

    // 4. 권한 그룹
    public static class Role {
        public static final String USER = "USER";
        public static final String ADMIN = "ADMIN";
    }
}