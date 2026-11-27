package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.CreateRequest;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.MenuPermission;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.RoleResponse;
import com.jiburo.server.domain.admin.repository.AdminRoleRepository;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 JPA(H2)로 역할/메뉴 권한 저장 동작 검증 — 메뉴 권한 교체 시 flush 순서(유니크 제약), 삭제 시 회수 등.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminRoleServiceIntegrationTest {

    @Autowired private AdminRoleService adminRoleService;
    @Autowired private AdminAuthorizationService adminAuthorizationService;
    @Autowired private AdminRoleRepository adminRoleRepository;
    @Autowired private UserAdminRoleRepository userAdminRoleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private Long idOf(RoleResponse role) {
        return HashidsUtils.decode(role.id());
    }

    private User saveUser(String oauthId) {
        return userRepository.save(User.builder().oauthId(oauthId).nickname(oauthId).roleCode(RoleType.ADMIN).build());
    }

    @Test
    void SUPER_ADMIN_시스템_역할은_시작시_생성되고_수정할_수_없다() {
        AdminRole superAdmin = adminRoleRepository.findByCode(AdminRole.SUPER_ADMIN_CODE).orElseThrow();

        assertThatThrownBy(() -> adminRoleService.updateMenus(superAdmin.getId(), List.of()))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.ADMIN_SYSTEM_ROLE_IMMUTABLE));
        assertThatThrownBy(() -> adminRoleService.delete(superAdmin.getId()))
                .isInstanceOf(JiburoException.class);
    }

    @Test
    void 메뉴_권한을_여러_번_교체해도_유니크_제약에_걸리지_않는다() {
        RoleResponse role = adminRoleService.create(new CreateRequest("TRANSLATOR", "번역 담당", null));

        adminRoleService.updateMenus(idOf(role), List.of(
                new MenuPermission(AdminMenu.I18N, true, false),
                new MenuPermission(AdminMenu.COMMON_CODE, true, false)));
        // 같은 메뉴를 다시 포함해 교체 (INSERT가 DELETE보다 먼저 나가면 실패하던 경우)
        RoleResponse updated = adminRoleService.updateMenus(idOf(role), List.of(
                new MenuPermission(AdminMenu.I18N, true, true),
                new MenuPermission(AdminMenu.COMMON_CODE, false, false)));
        em.flush();
        em.clear();

        assertThat(updated.menus()).filteredOn(m -> m.menu() == AdminMenu.I18N)
                .singleElement().satisfies(m -> assertThat(m.write()).isTrue());
        assertThat(updated.menus()).filteredOn(m -> m.menu() == AdminMenu.COMMON_CODE)
                .singleElement().satisfies(m -> assertThat(m.read()).isFalse());
        assertThat(adminRoleService.find(idOf(role)).menus()).filteredOn(MenuPermission::read).hasSize(1);
    }

    @Test
    void 편집을_주면_노출도_함께_적용되고_권한_변경은_해당_계정에_즉시_반영된다() {
        RoleResponse role = adminRoleService.create(new CreateRequest("CS_MANAGER", "CS 담당", null));
        User user = saveUser("kakao_cs");
        userAdminRoleRepository.save(new UserAdminRole(user, adminRoleRepository.findById(idOf(role)).orElseThrow()));

        assertThat(adminAuthorizationService.can(user.getId(), AdminMenu.POST, AdminAccessLevel.READ)).isFalse();

        adminRoleService.updateMenus(idOf(role), List.of(new MenuPermission(AdminMenu.POST, false, true)));

        assertThat(adminAuthorizationService.can(user.getId(), AdminMenu.POST, AdminAccessLevel.READ)).isTrue();
        assertThat(adminAuthorizationService.can(user.getId(), AdminMenu.POST, AdminAccessLevel.WRITE)).isTrue();
    }

    @Test
    void SUPER_ADMIN_전용_메뉴나_조회전용_메뉴의_편집은_부여할_수_없다() {
        RoleResponse role = adminRoleService.create(new CreateRequest("OPERATOR", "운영 담당", null));

        assertThatThrownBy(() -> adminRoleService.updateMenus(idOf(role), List.of(new MenuPermission(AdminMenu.ROLE, true, false))))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.ADMIN_MENU_NOT_GRANTABLE));
        assertThatThrownBy(() -> adminRoleService.updateMenus(idOf(role), List.of(new MenuPermission(AdminMenu.AUDIT_LOG, true, true))))
                .isInstanceOf(JiburoException.class);
    }

    @Test
    void 역할_코드는_형식과_중복을_검사한다() {
        adminRoleService.create(new CreateRequest("OPERATOR", "운영", null));

        assertThatThrownBy(() -> adminRoleService.create(new CreateRequest("OPERATOR", "중복", null)))
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.ADMIN_ROLE_CODE_DUPLICATE));
        assertThatThrownBy(() -> adminRoleService.create(new CreateRequest("operator-1", "형식", null)))
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.ADMIN_ROLE_CODE_INVALID));
    }

    @Test
    void 역할을_삭제하면_회수되고_남은_역할이_없는_계정은_일반_사용자가_된다() {
        RoleResponse a = adminRoleService.create(new CreateRequest("ROLE_A", "A", null));
        RoleResponse b = adminRoleService.create(new CreateRequest("ROLE_B", "B", null));
        User onlyA = saveUser("kakao_only_a");
        User both = saveUser("kakao_both");
        userAdminRoleRepository.save(new UserAdminRole(onlyA, adminRoleRepository.findById(idOf(a)).orElseThrow()));
        userAdminRoleRepository.save(new UserAdminRole(both, adminRoleRepository.findById(idOf(a)).orElseThrow()));
        userAdminRoleRepository.save(new UserAdminRole(both, adminRoleRepository.findById(idOf(b)).orElseThrow()));
        em.flush();

        adminRoleService.delete(idOf(a));
        em.flush();
        em.clear();

        assertThat(adminRoleRepository.findById(idOf(a))).isEmpty();
        assertThat(userRepository.findById(onlyA.getId()).orElseThrow().getRoleCode()).isEqualTo(RoleType.USER);
        assertThat(userRepository.findById(both.getId()).orElseThrow().getRoleCode()).isEqualTo(RoleType.ADMIN);
        assertThat(userAdminRoleRepository.rolesOf(both.getId())).extracting(AdminRole::getCode).containsExactly("ROLE_B");
    }
}
