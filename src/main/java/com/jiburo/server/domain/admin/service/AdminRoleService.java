package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.AdminRoleMenu;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.CreateRequest;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.MenuPermission;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.RoleResponse;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.UpdateRequest;
import com.jiburo.server.domain.admin.repository.AdminRoleRepository;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 관리자 역할 생성/수정/삭제 및 역할별 메뉴 권한 설정 (ROLE 메뉴 = SUPER_ADMIN 전용)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRoleService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    private final AdminRoleRepository adminRoleRepository;
    private final UserAdminRoleRepository userAdminRoleRepository;
    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public List<RoleResponse> findAll() {
        Map<Long, Long> counts = userAdminRoleRepository.countGroupByRole().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return adminRoleRepository.findAllWithMenus().stream()
                .map(role -> RoleResponse.of(role, counts.getOrDefault(role.getId(), 0L)))
                .toList();
    }

    public RoleResponse find(Long roleId) {
        AdminRole role = findOrThrow(roleId);
        return RoleResponse.of(role, userAdminRoleRepository.countByRole_Id(roleId));
    }

    @Transactional
    public RoleResponse create(CreateRequest request) {
        String code = request.code().trim();
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new JiburoException(ErrorCode.ADMIN_ROLE_CODE_INVALID);
        }
        if (adminRoleRepository.existsByCode(code)) {
            throw new JiburoException(ErrorCode.ADMIN_ROLE_CODE_DUPLICATE);
        }
        AdminRole role = adminRoleRepository.save(new AdminRole(code, request.name().trim(), request.description(), false));
        return RoleResponse.of(role, 0);
    }

    @Transactional
    public RoleResponse update(Long roleId, UpdateRequest request) {
        AdminRole role = findMutableOrThrow(roleId);
        role.rename(request.name(), request.description());
        return RoleResponse.of(role, userAdminRoleRepository.countByRole_Id(roleId));
    }

    /**
     * 메뉴 권한 전체 교체. write=true면 read도 부여, 조회 전용 메뉴에 write 요청은 무시하지 않고 거부한다.
     */
    @Transactional
    public RoleResponse updateMenus(Long roleId, List<MenuPermission> permissions) {
        AdminRole role = findMutableOrThrow(roleId);

        Map<AdminMenu, AdminRoleMenu> next = new EnumMap<>(AdminMenu.class);
        for (MenuPermission p : permissions) {
            if (!p.read() && !p.write()) continue; // 권한 없음 = 행 없음
            AdminMenu menu = p.menu();
            if (!menu.isGrantable() || (p.write() && !menu.isWritable())) {
                throw new JiburoException(ErrorCode.ADMIN_MENU_NOT_GRANTABLE);
            }
            next.put(menu, new AdminRoleMenu(role, menu, p.write()));
        }
        // Hibernate는 flush 시 INSERT를 DELETE보다 먼저 실행하므로, 같은 (role, menu)를 지우고 다시 넣으면
        // 유니크 제약에 걸린다 → 먼저 비우고 flush한 뒤 새 권한을 넣는다.
        role.replaceMenus(List.of());
        adminRoleRepository.flush();
        role.replaceMenus(new ArrayList<>(next.values()));
        adminRoleRepository.flush();

        adminAuthorizationService.invalidate(userAdminRoleRepository.findUserIdsByRoleId(roleId));
        return RoleResponse.of(role, userAdminRoleRepository.countByRole_Id(roleId));
    }

    /**
     * 역할 삭제. 부여된 계정에서 함께 회수하고, 남은 관리자 역할이 없는 계정은 일반 사용자(USER)로 되돌린다.
     */
    @Transactional
    public void delete(Long roleId) {
        AdminRole role = findMutableOrThrow(roleId);
        List<UUID> affected = userAdminRoleRepository.findUserIdsByRoleId(roleId);

        // bulk DELETE는 영속성 컨텍스트에 남은 연결 엔티티와 어긋나므로 엔티티 단위로 삭제
        userAdminRoleRepository.deleteAll(userAdminRoleRepository.findAllByRole_Id(roleId));
        userAdminRoleRepository.flush();
        adminRoleRepository.delete(role);
        adminRoleRepository.flush();

        for (UUID userId : affected) {
            if (userAdminRoleRepository.rolesOf(userId).isEmpty()) {
                userRepository.findById(userId).ifPresent(u -> u.changeRole(RoleType.USER));
            }
        }
        adminAuthorizationService.invalidate(affected);
    }

    private AdminRole findOrThrow(Long roleId) {
        return adminRoleRepository.findById(roleId)
                .orElseThrow(() -> new JiburoException(ErrorCode.ADMIN_ROLE_NOT_FOUND));
    }

    private AdminRole findMutableOrThrow(Long roleId) {
        AdminRole role = findOrThrow(roleId);
        if (role.isSystemRole()) {
            throw new JiburoException(ErrorCode.ADMIN_SYSTEM_ROLE_IMMUTABLE);
        }
        return role;
    }
}
