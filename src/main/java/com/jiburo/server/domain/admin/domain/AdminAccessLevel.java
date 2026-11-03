package com.jiburo.server.domain.admin.domain;

/**
 * 메뉴 접근 수준. WRITE는 READ를 포함한다.
 * - READ  = 메뉴 노출 + 조회 API
 * - WRITE = 변경 API (생성/수정/삭제)
 */
public enum AdminAccessLevel {
    READ, WRITE;

    public boolean covers(AdminAccessLevel required) {
        return this == WRITE || required == READ;
    }
}
