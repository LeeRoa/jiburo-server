package com.jiburo.server.domain.admin.controller;

/**
 * 관리자 API 공통 규약
 *
 * - 모든 관리자 컨트롤러는 {@link #BASE_PATH} 아래에 매핑한다.
 *   SecurityConfig에서 이 경로 전체를 ROLE_ADMIN 전용으로 보호하므로, 컨트롤러마다
 *   권한 검사를 따로 넣지 않아도 된다.
 * - 관리자 컨트롤러는 domain.admin.controller 에 두고, 비즈니스 로직은 각 도메인의 서비스를 재사용한다.
 *   관리자 전용 조회/변경 로직이 필요하면 domain.admin.service 에 둔다.
 * - 상태를 바꾸는 관리자 API에는 반드시 @AuditLog(action = LogActionType.ADMIN_XXX)를 붙여 이력을 남긴다.
 */
public final class AdminApi {

    public static final String BASE_PATH = "/api/v1/admin";

    private AdminApi() {
    }
}
