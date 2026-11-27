package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.AdminRoleMenu;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 역할·메뉴 권한 관리 API 요청/응답 DTO 모음
 */
public final class AdminRoleDtos {

    private AdminRoleDtos() {
    }

    public record MenuDefinition(
            @Schema(description = "메뉴 코드") AdminMenu menu,
            @Schema(description = "메뉴 이름") String label,
            @Schema(description = "관리자 웹 경로") String path,
            @Schema(description = "편집 권한 개념이 있는지 (false = 조회 전용 메뉴)") boolean writable,
            @Schema(description = "일반 역할에 부여 가능한지 (false = SUPER_ADMIN 전용)") boolean grantable
    ) {
        public static List<MenuDefinition> all() {
            return Arrays.stream(AdminMenu.values())
                    .map(m -> new MenuDefinition(m, m.getLabel(), m.getPath(), m.isWritable(), m.isGrantable()))
                    .toList();
        }
    }

    public record MenuPermission(
            @Schema(description = "메뉴 코드") @NotNull AdminMenu menu,
            @Schema(description = "노출(조회) 권한") boolean read,
            @Schema(description = "편집(변경) 권한 — true면 노출도 자동 부여") boolean write
    ) {}

    public record RoleResponse(
            @Schema(description = "역할 ID (인코딩된 문자열)") String id,
            @Schema(description = "역할 코드") String code,
            @Schema(description = "역할 이름") String name,
            @Schema(description = "설명") String description,
            @Schema(description = "시스템 역할 여부 (수정·삭제 불가)") boolean systemRole,
            @Schema(description = "이 역할을 가진 계정 수") long userCount,
            @Schema(description = "메뉴별 권한 (AdminMenu 정의 순서, 모든 메뉴 포함)") List<MenuPermission> menus
    ) {
        public static RoleResponse of(AdminRole role, long userCount) {
            Map<AdminMenu, AdminRoleMenu> granted = role.getMenus().stream()
                    .collect(Collectors.toMap(AdminRoleMenu::getMenu, Function.identity(), (a, b) -> a));
            List<MenuPermission> menus = Arrays.stream(AdminMenu.values())
                    .map(menu -> {
                        if (role.isSuperAdmin()) return new MenuPermission(menu, true, menu.isWritable());
                        AdminRoleMenu rm = granted.get(menu);
                        return new MenuPermission(menu, rm != null, rm != null && rm.isCanWrite());
                    })
                    .toList();
            return new RoleResponse(HashidsUtils.encode(role.getId()), role.getCode(), role.getName(),
                    role.getDescription(), role.isSystemRole(), userCount, menus);
        }
    }

    public record CreateRequest(
            @Schema(description = "역할 코드 (영문 대문자 시작, 대문자/숫자/_ 2~50자, 생성 후 변경 불가)", example = "TRANSLATOR")
            @NotBlank String code,
            @Schema(description = "역할 이름", example = "번역 담당") @NotBlank @Size(max = 100) String name,
            @Schema(description = "설명") @Size(max = 500) String description
    ) {}

    public record UpdateRequest(
            @Schema(description = "역할 이름 (null이면 유지)") @Size(max = 100) String name,
            @Schema(description = "설명 (null이면 유지)") @Size(max = 500) String description
    ) {}

    public record MenusUpdateRequest(
            @Schema(description = "메뉴별 권한. 목록에 없는 메뉴는 권한 없음(미노출)으로 처리") @NotNull List<@Valid MenuPermission> menus
    ) {}
}
