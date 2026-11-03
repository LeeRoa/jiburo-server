package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.AdminRoleMenu;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관리자 메뉴 권한 확인.
 *
 * - 권한은 JWT에 넣지 않고 매 요청 이 서비스로 확인한다 → 회수/변경이 토큰 재발급 없이 즉시 반영.
 * - 사용자별 결과를 메모리에 캐시하고, 역할/메뉴 권한이 바뀌면 invalidate. 스케일 아웃 시 다른 서버는
 *   주기적 전체 비우기(기본 60초)로 반영된다.
 * - SUPER_ADMIN 역할 보유자는 모든 메뉴 WRITE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final UserAdminRoleRepository userAdminRoleRepository;

    private final Map<UUID, AdminAccess> cache = new ConcurrentHashMap<>();

    public record RoleSummary(Long id, String code, String name, boolean systemRole) {}

    public record AdminAccess(boolean superAdmin, List<RoleSummary> roles, Map<AdminMenu, AdminAccessLevel> menus) {
        public boolean hasAnyRole() {
            return !roles.isEmpty();
        }

        public boolean can(AdminMenu menu, AdminAccessLevel required) {
            if (superAdmin) return true;
            AdminAccessLevel granted = menus.get(menu);
            return granted != null && granted.covers(required);
        }
    }

    public AdminAccess getAccess(UUID userId) {
        if (userId == null) {
            return new AdminAccess(false, List.of(), Map.of());
        }
        return cache.computeIfAbsent(userId, this::load);
    }

    public boolean can(UUID userId, AdminMenu menu, AdminAccessLevel level) {
        return getAccess(userId).can(menu, level);
    }

    public boolean hasAnyRole(UUID userId) {
        return getAccess(userId).hasAnyRole();
    }

    public boolean isSuperAdmin(UUID userId) {
        return getAccess(userId).superAdmin();
    }

    public void invalidate(UUID userId) {
        cache.remove(userId);
    }

    public void invalidate(Collection<UUID> userIds) {
        userIds.forEach(cache::remove);
    }

    @Scheduled(fixedDelayString = "${app.admin.permission-cache-ttl-ms:60000}")
    public void invalidateAll() {
        cache.clear();
    }

    private AdminAccess load(UUID userId) {
        List<AdminRole> roles = userAdminRoleRepository.findRolesWithMenusByUserId(userId);
        boolean superAdmin = roles.stream().anyMatch(AdminRole::isSuperAdmin);

        Map<AdminMenu, AdminAccessLevel> menus = new EnumMap<>(AdminMenu.class);
        for (AdminRole role : roles) {
            for (AdminRoleMenu rm : role.getMenus()) {
                // 여러 역할이 같은 메뉴를 주면 더 높은 수준(WRITE)을 적용
                menus.merge(rm.getMenu(), rm.level(),
                        (a, b) -> a == AdminAccessLevel.WRITE || b == AdminAccessLevel.WRITE ? AdminAccessLevel.WRITE : AdminAccessLevel.READ);
            }
        }
        List<RoleSummary> summaries = roles.stream()
                .map(r -> new RoleSummary(r.getId(), r.getCode(), r.getName(), r.isSystemRole()))
                .sorted(Comparator.comparing(RoleSummary::id))
                .toList();
        return new AdminAccess(superAdmin, summaries, Collections.unmodifiableMap(menus));
    }
}
