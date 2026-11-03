package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.service.AdminAuthorizationService.AdminAccess;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record AdminMeResponseDto(
        @Schema(description = "사용자 ID") UUID userId,
        @Schema(description = "SUPER_ADMIN 여부 (true면 모든 메뉴 편집 가능)") boolean superAdmin,
        @Schema(description = "보유 관리자 역할") List<Role> roles,
        @Schema(description = "노출할 메뉴 (AdminMenu 정의 순서). 이 목록으로 좌측 메뉴를 구성") List<Menu> menus
) {
    public record Role(
            @Schema(description = "역할 ID (인코딩된 문자열)") String id,
            @Schema(description = "역할 코드") String code,
            @Schema(description = "역할 이름") String name
    ) {}

    public record Menu(
            @Schema(description = "메뉴 코드 (AdminMenu)") AdminMenu menu,
            @Schema(description = "메뉴 이름") String label,
            @Schema(description = "관리자 웹 경로") String path,
            @Schema(description = "편집(변경) 가능 여부 — false면 저장/삭제 버튼 숨김") boolean canWrite
    ) {}

    public static AdminMeResponseDto of(UUID userId, AdminAccess access) {
        List<Menu> menus = Arrays.stream(AdminMenu.values())
                .filter(menu -> access.can(menu, AdminAccessLevel.READ))
                .map(menu -> new Menu(menu, menu.getLabel(), menu.getPath(),
                        menu.isWritable() && access.can(menu, AdminAccessLevel.WRITE)))
                .toList();
        List<Role> roles = access.roles().stream()
                .map(r -> new Role(HashidsUtils.encode(r.id()), r.code(), r.name()))
                .toList();
        return new AdminMeResponseDto(userId, access.superAdmin(), roles, menus);
    }
}
