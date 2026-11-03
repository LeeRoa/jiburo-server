package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminRoleDtos.CreateRequest;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.MenuDefinition;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.MenusUpdateRequest;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.RoleResponse;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.UpdateRequest;
import com.jiburo.server.domain.admin.service.AdminRoleService;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 - 역할·권한 관리 (ROLE 메뉴 = SUPER_ADMIN 전용)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH)
@Tag(name = "Admin-Role", description = "[관리자] 역할·메뉴 권한 관리 API (SUPER_ADMIN 전용)")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "[관리자] 관리자 메뉴 정의 목록", description = "역할에 부여할 수 있는 메뉴와 각 메뉴의 편집 가능 여부")
    @GetMapping("/menus")
    @PreAuthorize("@adminAccess.read('ROLE')")
    public ApiResponse<List<MenuDefinition>> getMenus() {
        return ApiResponse.success(MenuDefinition.all());
    }

    @Operation(summary = "[관리자] 역할 목록", description = "역할별 메뉴 권한과 소속 계정 수")
    @GetMapping("/roles")
    @PreAuthorize("@adminAccess.read('ROLE')")
    public ApiResponse<List<RoleResponse>> getRoles() {
        return ApiResponse.success(adminRoleService.findAll());
    }

    @Operation(summary = "[관리자] 역할 단건 조회")
    @GetMapping("/roles/{roleId}")
    @PreAuthorize("@adminAccess.read('ROLE')")
    public ApiResponse<RoleResponse> getRole(@PathVariable String roleId) {
        return ApiResponse.success(adminRoleService.find(HashidsUtils.decode(roleId)));
    }

    @Operation(summary = "[관리자] 역할 생성", description = "생성 직후에는 어떤 메뉴도 노출되지 않습니다. 메뉴 권한은 별도로 설정합니다.")
    @PostMapping("/roles")
    @PreAuthorize("@adminAccess.write('ROLE')")
    @AuditLog(action = LogActionType.ADMIN_ROLE_CREATE)
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(adminRoleService.create(request));
    }

    @Operation(summary = "[관리자] 역할 이름/설명 수정", description = "시스템 역할(SUPER_ADMIN)은 수정할 수 없습니다.")
    @PatchMapping("/roles/{roleId}")
    @PreAuthorize("@adminAccess.write('ROLE')")
    @AuditLog(action = LogActionType.ADMIN_ROLE_UPDATE)
    public ApiResponse<RoleResponse> updateRole(@PathVariable String roleId, @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.success(adminRoleService.update(HashidsUtils.decode(roleId), request));
    }

    @Operation(summary = "[관리자] 역할 메뉴 권한 설정",
            description = "역할의 메뉴별 노출(조회)/편집(변경) 권한을 통째로 교체합니다. 목록에 없는 메뉴는 미노출. "
                    + "이 역할을 가진 계정에 즉시 반영됩니다.")
    @PutMapping("/roles/{roleId}/menus")
    @PreAuthorize("@adminAccess.write('ROLE')")
    @AuditLog(action = LogActionType.ADMIN_ROLE_MENU_UPDATE)
    public ApiResponse<RoleResponse> updateRoleMenus(@PathVariable String roleId, @Valid @RequestBody MenusUpdateRequest request) {
        return ApiResponse.success(adminRoleService.updateMenus(HashidsUtils.decode(roleId), request.menus()));
    }

    @Operation(summary = "[관리자] 역할 삭제",
            description = "부여된 계정에서 함께 회수됩니다. 남은 관리자 역할이 없는 계정은 일반 사용자가 됩니다.")
    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("@adminAccess.write('ROLE')")
    @AuditLog(action = LogActionType.ADMIN_ROLE_DELETE)
    public ApiResponse<Void> deleteRole(@PathVariable String roleId) {
        adminRoleService.delete(HashidsUtils.decode(roleId));
        return ApiResponse.success();
    }
}
