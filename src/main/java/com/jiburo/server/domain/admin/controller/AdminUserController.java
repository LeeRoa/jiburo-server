package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminUserDtos.RolesUpdateRequest;
import com.jiburo.server.domain.admin.dto.AdminUserDtos.SearchCondition;
import com.jiburo.server.domain.admin.dto.AdminUserDtos.UserResponse;
import com.jiburo.server.domain.admin.service.AdminUserService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/users")
@Tag(name = "Admin-User", description = "[관리자] 사용자 관리 API")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "[관리자] 사용자 목록 검색", description = "닉네임/이메일/소셜 ID, 가입 경로, 관리자 여부, 보유 역할로 검색 (가입일 최신순)")
    @GetMapping
    @PreAuthorize("@adminAccess.read('USER')")
    public ApiResponse<Page<UserResponse>> search(
            @ParameterObject @ModelAttribute SearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(adminUserService.search(condition, pageable));
    }

    @Operation(summary = "[관리자] 사용자 상세")
    @GetMapping("/{userId}")
    @PreAuthorize("@adminAccess.read('USER')")
    public ApiResponse<UserResponse> find(@PathVariable UUID userId) {
        return ApiResponse.success(adminUserService.find(userId));
    }

    @Operation(summary = "[관리자] 관리자 역할 부여/회수",
            description = "대상 계정의 관리자 역할을 요청 목록으로 교체합니다 (빈 목록 = 모두 회수 → 일반 사용자). "
                    + "본인 변경 불가, SUPER_ADMIN은 SUPER_ADMIN만 부여/회수, 본인 권한보다 넓은 역할은 부여/회수 불가, "
                    + "마지막 SUPER_ADMIN 회수 불가. 회수는 즉시 적용, 새로 부여된 계정은 다시 로그인하면 관리자 웹에 진입할 수 있습니다.")
    @PutMapping("/{userId}/admin-roles")
    @PreAuthorize("@adminAccess.write('USER')")
    @AuditLog(action = LogActionType.ADMIN_USER_ROLE_CHANGE)
    public ApiResponse<UserResponse> updateRoles(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User admin,
            @PathVariable UUID userId,
            @Valid @RequestBody RolesUpdateRequest request
    ) {
        return ApiResponse.success(adminUserService.updateRoles(admin.getUserId(), userId, request.roleIds()));
    }
}
