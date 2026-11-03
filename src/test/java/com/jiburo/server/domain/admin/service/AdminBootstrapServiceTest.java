package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class AdminBootstrapServiceTest {

    private UserAdminRoleRepository userAdminRoleRepository;
    private AdminRoleInitializer initializer;
    private AdminAuthorizationService authorizationService;
    private UserRepository userRepository;
    private AdminRole superAdmin;

    @BeforeEach
    void setUp() {
        userAdminRoleRepository = mock(UserAdminRoleRepository.class);
        initializer = mock(AdminRoleInitializer.class);
        authorizationService = mock(AdminAuthorizationService.class);
        userRepository = mock(UserRepository.class);
        superAdmin = new AdminRole(AdminRole.SUPER_ADMIN_CODE, "최고 관리자", null, true);
        ReflectionTestUtils.setField(superAdmin, "id", 1L);
        given(initializer.ensureSuperAdminRole()).willReturn(superAdmin);
    }

    private AdminBootstrapService service(String ids) {
        return new AdminBootstrapService(List.of(ids.split(",")), userAdminRoleRepository, initializer,
                authorizationService, userRepository);
    }

    private User user(String oauthId) {
        User user = User.builder().oauthId(oauthId).nickname("n").roleCode(RoleType.USER).build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @Test
    void 지정된_계정은_로그인시_SUPER_ADMIN과_ADMIN_계정구분을_받는다() {
        User user = user("kakao_1");
        given(userAdminRoleRepository.existsByUser_IdAndRole_Id(user.getId(), 1L)).willReturn(false);

        service("kakao_1, google_2").applyOnLogin(user);

        verify(userAdminRoleRepository).save(any(UserAdminRole.class));
        assertThat(user.getRoleCode()).isEqualTo(RoleType.ADMIN);
        verify(authorizationService).invalidate(user.getId());
    }

    @Test
    void 이미_SUPER_ADMIN이면_중복_부여하지_않는다() {
        User user = user("kakao_1");
        given(userAdminRoleRepository.existsByUser_IdAndRole_Id(user.getId(), 1L)).willReturn(true);

        service("kakao_1").applyOnLogin(user);

        verify(userAdminRoleRepository, never()).save(any());
    }

    @Test
    void 지정되지_않은_계정은_DB를_건드리지_않는다() {
        User user = user("naver_9");

        service("kakao_1").applyOnLogin(user);
        service("").applyOnLogin(user);

        verifyNoInteractions(initializer, userAdminRoleRepository, userRepository);
        assertThat(user.getRoleCode()).isEqualTo(RoleType.USER);
    }
}
