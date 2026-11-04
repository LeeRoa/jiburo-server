package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import com.jiburo.server.domain.admin.dto.AdminUserDtos.SearchCondition;
import com.jiburo.server.domain.admin.dto.AdminUserDtos.UserResponse;
import com.jiburo.server.domain.admin.repository.AdminRoleRepository;
import com.jiburo.server.domain.admin.repository.AdminUserQueryRepository;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.admin.service.AdminAuthorizationService.AdminAccess;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.jiburo.server.domain.user.service.UserSuspensionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 - 사용자 조회 및 관리자 역할 부여/회수
 *
 * 권한 상승 방지 규칙:
 * 1. 본인 역할은 변경할 수 없다
 * 2. SUPER_ADMIN 역할은 SUPER_ADMIN만 부여/회수할 수 있다
 * 3. SUPER_ADMIN이 아닌 관리자는 자기가 가진 권한 범위 안의 역할만 부여/회수할 수 있다
 *    (예: 사용자 메뉴 편집 권한만 있는 CS 담당이 운영 설정 편집 역할을 남에게 주는 것 차단)
 * 4. 마지막 SUPER_ADMIN의 역할은 회수할 수 없다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserQueryRepository adminUserQueryRepository;
    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final UserAdminRoleRepository userAdminRoleRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final UserSuspensionService userSuspensionService;

    public Page<UserResponse> search(SearchCondition condition, Pageable pageable) {
        Long roleId = StringUtils.hasText(condition.adminRoleId()) ? HashidsUtils.decode(condition.adminRoleId()) : null;
        Page<User> users = adminUserQueryRepository.search(new AdminUserQueryRepository.Condition(
                condition.keyword(), condition.provider(), condition.adminOnly(), roleId), pageable);

        List<UUID> ids = users.getContent().stream().map(User::getId).toList();
        Map<UUID, List<AdminRole>> rolesByUser = ids.isEmpty() ? Map.of()
                : userAdminRoleRepository.findAllByUserIdInWithRole(ids).stream()
                .collect(Collectors.groupingBy(ur -> ur.getUser().getId(),
                        Collectors.mapping(UserAdminRole::getRole, Collectors.toList())));

        return users.map(u -> UserResponse.of(u, rolesByUser.getOrDefault(u.getId(), List.of())));
    }

    public UserResponse find(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.of(user, userAdminRoleRepository.rolesOf(userId));
    }

    /** 계정 정지 (days가 null이면 영구). 관리자 계정·본인은 불가 */
    @Transactional
    public UserResponse suspend(UUID actorId, UUID userId, Integer days, String reason) {
        userSuspensionService.suspend(actorId, userId, days, reason == null || reason.isBlank() ? null : reason.trim());
        return find(userId);
    }

    @Transactional
    public UserResponse unsuspend(UUID userId) {
        userSuspensionService.unsuspend(userId);
        return find(userId);
    }

    /**
     * 대상 계정의 관리자 역할을 요청 목록으로 통째로 교체한다.
     */
    @Transactional
    public UserResponse updateRoles(UUID actorId, UUID targetId, List<String> roleIdHashes) {
        if (actorId.equals(targetId)) {
            throw new JiburoException(ErrorCode.ADMIN_CANNOT_CHANGE_OWN_ROLES);
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));

        Set<Long> requestedIds = roleIdHashes.stream().map(HashidsUtils::decode).collect(Collectors.toSet());
        List<AdminRole> requested = adminRoleRepository.findAllById(requestedIds);
        if (requested.size() != requestedIds.size()) {
            throw new JiburoException(ErrorCode.ADMIN_ROLE_NOT_FOUND);
        }

        List<UserAdminRole> current = userAdminRoleRepository.findAllByUserIdWithRole(targetId);
        Set<Long> currentIds = current.stream().map(ur -> ur.getRole().getId()).collect(Collectors.toSet());

        List<AdminRole> toAdd = requested.stream().filter(r -> !currentIds.contains(r.getId())).toList();
        List<UserAdminRole> toRemove = current.stream().filter(ur -> !requestedIds.contains(ur.getRole().getId())).toList();

        AdminAccess actor = adminAuthorizationService.getAccess(actorId);
        List<AdminRole> changed = new ArrayList<>(toAdd);
        toRemove.forEach(ur -> changed.add(ur.getRole()));
        for (AdminRole role : changed) {
            validateActorCanManage(actor, role);
        }

        boolean removesSuperAdmin = toRemove.stream().anyMatch(ur -> ur.getRole().isSuperAdmin());
        if (removesSuperAdmin) {
            Long superId = toRemove.stream().map(UserAdminRole::getRole).filter(AdminRole::isSuperAdmin)
                    .findFirst().orElseThrow().getId();
            if (userAdminRoleRepository.countByRole_Id(superId) <= 1) {
                throw new JiburoException(ErrorCode.ADMIN_LAST_SUPER_ADMIN);
            }
        }

        userAdminRoleRepository.deleteAll(toRemove);
        userAdminRoleRepository.flush();
        toAdd.forEach(role -> userAdminRoleRepository.save(new UserAdminRole(target, role)));

        target.changeRole(requested.isEmpty() ? RoleType.USER : RoleType.ADMIN);
        adminAuthorizationService.invalidate(targetId);
        return UserResponse.of(target, requested);
    }

    private void validateActorCanManage(AdminAccess actor, AdminRole role) {
        if (actor.superAdmin()) return;
        if (role.isSuperAdmin()) {
            throw new JiburoException(ErrorCode.ADMIN_SUPER_ADMIN_ONLY);
        }
        boolean withinOwnPermission = role.getMenus().stream()
                .allMatch(rm -> actor.can(rm.getMenu(), rm.level()));
        if (!withinOwnPermission) {
            throw new JiburoException(ErrorCode.ADMIN_ROLE_EXCEEDS_OWN_PERMISSION);
        }
    }
}
