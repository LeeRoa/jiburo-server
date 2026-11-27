package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.AdminRoleMenu;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AdminAuthorizationServiceTest {

    private UserAdminRoleRepository repository;
    private AdminAuthorizationService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(UserAdminRoleRepository.class);
        service = new AdminAuthorizationService(repository);
    }

    private AdminRole role(long id, String code, Object... menuAndWrite) {
        AdminRole role = new AdminRole(code, code, null, false);
        ReflectionTestUtils.setField(role, "id", id);
        List<AdminRoleMenu> menus = new java.util.ArrayList<>();
        for (int i = 0; i < menuAndWrite.length; i += 2) {
            menus.add(new AdminRoleMenu(role, (AdminMenu) menuAndWrite[i], (Boolean) menuAndWrite[i + 1]));
        }
        role.replaceMenus(menus);
        return role;
    }

    @Test
    void 역할이_없으면_어떤_메뉴도_접근_불가() {
        given(repository.findRolesWithMenusByUserId(userId)).willReturn(List.of());

        assertThat(service.hasAnyRole(userId)).isFalse();
        assertThat(service.can(userId, AdminMenu.SETTING, AdminAccessLevel.READ)).isFalse();
    }

    @Test
    void 노출만_준_메뉴는_조회만_가능하고_부여하지_않은_메뉴는_불가() {
        given(repository.findRolesWithMenusByUserId(userId)).willReturn(List.of(
                role(1, "TRANSLATOR", AdminMenu.COMMON_CODE, false, AdminMenu.I18N, true)));

        assertThat(service.can(userId, AdminMenu.COMMON_CODE, AdminAccessLevel.READ)).isTrue();
        assertThat(service.can(userId, AdminMenu.COMMON_CODE, AdminAccessLevel.WRITE)).isFalse();
        assertThat(service.can(userId, AdminMenu.I18N, AdminAccessLevel.WRITE)).isTrue();
        assertThat(service.can(userId, AdminMenu.SETTING, AdminAccessLevel.READ)).isFalse();
    }

    @Test
    void 여러_역할이면_더_높은_권한을_합산한다() {
        given(repository.findRolesWithMenusByUserId(userId)).willReturn(List.of(
                role(1, "A", AdminMenu.POST, false),
                role(2, "B", AdminMenu.POST, true, AdminMenu.USER, false)));

        assertThat(service.can(userId, AdminMenu.POST, AdminAccessLevel.WRITE)).isTrue();
        assertThat(service.can(userId, AdminMenu.USER, AdminAccessLevel.READ)).isTrue();
        assertThat(service.getAccess(userId).roles()).hasSize(2);
    }

    @Test
    void SUPER_ADMIN은_모든_메뉴_편집_가능() {
        AdminRole superAdmin = new AdminRole(AdminRole.SUPER_ADMIN_CODE, "최고 관리자", null, true);
        ReflectionTestUtils.setField(superAdmin, "id", 1L);
        given(repository.findRolesWithMenusByUserId(userId)).willReturn(List.of(superAdmin));

        for (AdminMenu menu : AdminMenu.values()) {
            assertThat(service.can(userId, menu, AdminAccessLevel.WRITE)).as(menu.name()).isTrue();
        }
    }

    @Test
    void 결과는_캐시되고_invalidate하면_다시_조회한다() {
        given(repository.findRolesWithMenusByUserId(userId)).willReturn(List.of());

        service.hasAnyRole(userId);
        service.hasAnyRole(userId);
        verify(repository, times(1)).findRolesWithMenusByUserId(userId);

        service.invalidate(userId);
        service.hasAnyRole(userId);
        verify(repository, times(2)).findRolesWithMenusByUserId(userId);
    }

    @Test
    void 비로그인은_권한_없음() {
        assertThat(service.can(null, AdminMenu.DASHBOARD, AdminAccessLevel.READ)).isFalse();
    }
}
