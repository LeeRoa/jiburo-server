package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.CreateRequest;
import com.jiburo.server.domain.admin.dto.AdminRoleDtos.MenuPermission;
import com.jiburo.server.domain.admin.dto.AdminUserDtos.SearchCondition;
import com.jiburo.server.domain.admin.dto.AdminUserDtos.UserResponse;
import com.jiburo.server.domain.admin.repository.AdminRoleRepository;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserServiceIntegrationTest {

    @Autowired private AdminUserService adminUserService;
    @Autowired private AdminRoleService adminRoleService;
    @Autowired private AdminAuthorizationService adminAuthorizationService;
    @Autowired private AdminRoleRepository adminRoleRepository;
    @Autowired private UserAdminRoleRepository userAdminRoleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private AdminRole superAdmin;
    private String csRoleId;        // USER 편집 + POST 편집
    private String operatorRoleId;  // SETTING 편집
    private User root;              // SUPER_ADMIN
    private User cs;                // CS 역할
    private User member;            // 일반 사용자

    @BeforeEach
    void setUp() {
        adminAuthorizationService.invalidateAll();
        superAdmin = adminRoleRepository.findByCode(AdminRole.SUPER_ADMIN_CODE).orElseThrow();
        csRoleId = adminRoleService.create(new CreateRequest("CS_MANAGER", "CS 담당", null)).id();
        adminRoleService.updateMenus(HashidsUtils.decode(csRoleId), List.of(
                new MenuPermission(AdminMenu.USER, true, true), new MenuPermission(AdminMenu.POST, true, true)));
        operatorRoleId = adminRoleService.create(new CreateRequest("OPERATOR", "운영 담당", null)).id();
        adminRoleService.updateMenus(HashidsUtils.decode(operatorRoleId), List.of(
                new MenuPermission(AdminMenu.SETTING, true, true)));

        root = save("kakao_root", "루트", RoleType.ADMIN);
        cs = save("google_cs", "씨에스", RoleType.ADMIN);
        member = save("naver_member", "회원", RoleType.USER);
        userAdminRoleRepository.save(new UserAdminRole(root, superAdmin));
        userAdminRoleRepository.save(new UserAdminRole(cs, adminRoleRepository.findById(HashidsUtils.decode(csRoleId)).orElseThrow()));
        em.flush();
    }

    private User save(String oauthId, String nickname, RoleType role) {
        return userRepository.save(User.builder().oauthId(oauthId).nickname(nickname).email(oauthId + "@t.com").roleCode(role).build());
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(expected));
    }

    @Test
    void SUPER_ADMIN은_역할을_부여하고_회수하며_계정_구분도_따라간다() {
        UserResponse granted = adminUserService.updateRoles(root.getId(), member.getId(), List.of(operatorRoleId));
        assertThat(granted.adminRoles()).extracting("code").containsExactly("OPERATOR");
        assertThat(userRepository.findById(member.getId()).orElseThrow().getRoleCode()).isEqualTo(RoleType.ADMIN);

        adminUserService.updateRoles(root.getId(), member.getId(), List.of());
        assertThat(userRepository.findById(member.getId()).orElseThrow().getRoleCode()).isEqualTo(RoleType.USER);
        assertThat(userAdminRoleRepository.rolesOf(member.getId())).isEmpty();
    }

    @Test
    void 본인_역할은_바꿀_수_없다() {
        assertError(() -> adminUserService.updateRoles(root.getId(), root.getId(), List.of()), ErrorCode.ADMIN_CANNOT_CHANGE_OWN_ROLES);
    }

    @Test
    void SUPER_ADMIN이_아니면_SUPER_ADMIN을_부여할_수_없다() {
        assertError(() -> adminUserService.updateRoles(cs.getId(), member.getId(),
                List.of(HashidsUtils.encode(superAdmin.getId()))), ErrorCode.ADMIN_SUPER_ADMIN_ONLY);
    }

    @Test
    void 본인_권한보다_넓은_역할은_부여할_수_없다() {
        // CS 담당(사용자·게시글 편집)이 운영 설정 편집 역할을 주려는 경우 → 권한 상승 차단
        assertError(() -> adminUserService.updateRoles(cs.getId(), member.getId(), List.of(operatorRoleId)),
                ErrorCode.ADMIN_ROLE_EXCEEDS_OWN_PERMISSION);
    }

    @Test
    void 본인_권한_범위_안의_역할은_부여할_수_있다() {
        UserResponse result = adminUserService.updateRoles(cs.getId(), member.getId(), List.of(csRoleId));

        assertThat(result.adminRoles()).extracting("code").containsExactly("CS_MANAGER");
    }

    @Test
    void SUPER_ADMIN끼리는_서로_회수할_수_있고_SUPER_ADMIN이_아니면_회수할_수_없다() {
        User root2 = save("kakao_root2", "루트2", RoleType.ADMIN);
        userAdminRoleRepository.save(new UserAdminRole(root2, superAdmin));
        em.flush();

        // CS 담당은 SUPER_ADMIN을 회수할 수 없음
        assertError(() -> adminUserService.updateRoles(cs.getId(), root2.getId(), List.of()), ErrorCode.ADMIN_SUPER_ADMIN_ONLY);

        // 다른 SUPER_ADMIN은 회수 가능 → SUPER_ADMIN 1명 남음
        adminUserService.updateRoles(root.getId(), root2.getId(), List.of());
        em.flush();
        assertThat(userAdminRoleRepository.countByRole_Id(superAdmin.getId())).isEqualTo(1);
        // 남은 1명(root)은 본인 변경 금지 + 다른 SUPER_ADMIN 부재로 어떤 경로로도 회수되지 않음.
        // (ADMIN_LAST_SUPER_ADMIN은 동시 요청 등 예외 상황을 막는 방어 규칙)
    }

    @Test
    void 없는_역할_ID는_ADMIN_ROLE_NOT_FOUND() {
        assertError(() -> adminUserService.updateRoles(root.getId(), member.getId(), List.of(HashidsUtils.encode(999999L))),
                ErrorCode.ADMIN_ROLE_NOT_FOUND);
    }

    @Test
    void 검색은_키워드_가입경로_관리자여부_역할로_필터링된다() {
        Page<UserResponse> admins = adminUserService.search(new SearchCondition(null, null, true, null), PageRequest.of(0, 20));
        assertThat(admins.getContent()).extracting(UserResponse::nickname).contains("루트", "씨에스").doesNotContain("회원");

        Page<UserResponse> byRole = adminUserService.search(new SearchCondition(null, null, null, csRoleId), PageRequest.of(0, 20));
        assertThat(byRole.getContent()).extracting(UserResponse::nickname).containsExactly("씨에스");
        assertThat(byRole.getContent().get(0).adminRoles()).extracting("code").containsExactly("CS_MANAGER");

        Page<UserResponse> byProvider = adminUserService.search(new SearchCondition("회", "naver", null, null), PageRequest.of(0, 20));
        assertThat(byProvider.getContent()).extracting(UserResponse::provider).containsExactly("naver");
    }
}
