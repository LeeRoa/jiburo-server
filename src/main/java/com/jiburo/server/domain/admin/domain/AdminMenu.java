package com.jiburo.server.domain.admin.domain;

import lombok.Getter;

/**
 * 관리자 웹 메뉴 정의 (단일 진실 공급원).
 *
 * - 메뉴 = 권한의 단위. 역할마다 메뉴별로 "노출(READ)" / "편집(WRITE)"을 부여한다.
 * - 새 관리자 기능을 만들면 여기에 메뉴를 추가하고, 그 기능의 관리자 API에
 *   {@code @PreAuthorize("@adminAccess.read('MENU')")} / {@code write('MENU')}를 붙인다.
 *   새 메뉴는 어떤 역할에도 자동으로 열리지 않는다 (SUPER_ADMIN 제외).
 * - writable=false: 조회 전용 메뉴 (편집 권한 개념 없음)
 * - grantable=false: 일반 역할에 부여할 수 없는 메뉴 (SUPER_ADMIN 전용)
 */
@Getter
public enum AdminMenu {

    DASHBOARD("대시보드", "/dashboard", false, true),
    SETTING("운영 설정", "/settings", true, true),
    COMMON_CODE("공통코드", "/common-codes", true, true),
    I18N("다국어 문구", "/i18n", true, true),
    USER("사용자", "/users", true, true),
    POST("게시글", "/posts", true, true),
    REPORT("신고", "/reports", true, true),
    EMOTICON("이모티콘", "/emoticons", true, true),
    BANNED_WORD("금지어", "/banned-words", true, true),
    AUDIT_LOG("감사 로그", "/audit-logs", false, true),
    ROLE("역할·권한", "/roles", true, false);

    private final String label;
    private final String path;
    private final boolean writable;
    private final boolean grantable;

    AdminMenu(String label, String path, boolean writable, boolean grantable) {
        this.label = label;
        this.path = path;
        this.writable = writable;
        this.grantable = grantable;
    }
}
