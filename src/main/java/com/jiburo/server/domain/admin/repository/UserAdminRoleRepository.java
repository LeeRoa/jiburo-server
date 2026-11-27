package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserAdminRoleRepository extends JpaRepository<UserAdminRole, Long> {

    // 권한 확인용: 사용자의 역할 + 메뉴 권한을 한 번에 조회
    @Query("SELECT DISTINCT r FROM UserAdminRole ur JOIN ur.role r LEFT JOIN FETCH r.menus WHERE ur.user.id = :userId")
    List<AdminRole> findRolesWithMenusByUserId(@Param("userId") UUID userId);

    @Query("SELECT ur FROM UserAdminRole ur JOIN FETCH ur.role WHERE ur.user.id = :userId")
    List<UserAdminRole> findAllByUserIdWithRole(@Param("userId") UUID userId);

    // 사용자 목록 화면용: 여러 사용자의 역할을 한 번에 (N+1 방지)
    @Query("SELECT ur FROM UserAdminRole ur JOIN FETCH ur.role WHERE ur.user.id IN :userIds")
    List<UserAdminRole> findAllByUserIdInWithRole(@Param("userIds") Collection<UUID> userIds);

    boolean existsByUser_IdAndRole_Id(UUID userId, Long roleId);

    List<UserAdminRole> findAllByRole_Id(Long roleId);

    long countByRole_Id(Long roleId);

    @Query("SELECT ur.role.id, COUNT(ur) FROM UserAdminRole ur GROUP BY ur.role.id")
    List<Object[]> countGroupByRole();

    @Query("SELECT DISTINCT ur.user.id FROM UserAdminRole ur WHERE ur.role.id = :roleId")
    List<UUID> findUserIdsByRoleId(@Param("roleId") Long roleId);

    default List<AdminRole> rolesOf(UUID userId) {
        return findAllByUserIdWithRole(userId).stream().map(UserAdminRole::getRole).toList();
    }
}
