package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.domain.UserAdminRole;
import com.jiburo.server.domain.admin.repository.UserAdminRoleRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 초기 관리자 지정: 환경변수 ADMIN_OAUTH_IDS(app.admin.bootstrap-oauth-ids, 콤마 구분, 예: kakao_123,google_456)에
 * 적힌 계정은 로그인할 때 SUPER_ADMIN 역할을 자동으로 받는다.
 * 관리자 웹에서 역할을 부여할 사람이 아무도 없는 최초 상태를 풀기 위한 장치이며, 이후 관리자는 관리자 웹에서 지정한다.
 */
@Slf4j
@Service
public class AdminBootstrapService {

    private final Set<String> bootstrapOauthIds;
    private final UserAdminRoleRepository userAdminRoleRepository;
    private final AdminRoleInitializer adminRoleInitializer;
    private final AdminAuthorizationService adminAuthorizationService;
    private final UserRepository userRepository;

    public AdminBootstrapService(
            @Value("${app.admin.bootstrap-oauth-ids:}") List<String> bootstrapOauthIds,
            UserAdminRoleRepository userAdminRoleRepository,
            AdminRoleInitializer adminRoleInitializer,
            AdminAuthorizationService adminAuthorizationService,
            UserRepository userRepository
    ) {
        this.bootstrapOauthIds = bootstrapOauthIds.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.userAdminRoleRepository = userAdminRoleRepository;
        this.adminRoleInitializer = adminRoleInitializer;
        this.adminAuthorizationService = adminAuthorizationService;
        this.userRepository = userRepository;
    }

    /**
     * 로그인 시점에 호출. 대상이 아니면 DB 접근 없이 바로 반환한다.
     * 호출자의 트랜잭션에 참여한다 (방금 가입해 아직 커밋되지 않은 사용자도 처리 가능).
     */
    @Transactional
    public void applyOnLogin(User user) {
        if (bootstrapOauthIds.isEmpty() || !bootstrapOauthIds.contains(user.getOauthId())) {
            return;
        }
        AdminRole superAdmin = adminRoleInitializer.ensureSuperAdminRole();
        if (!userAdminRoleRepository.existsByUser_IdAndRole_Id(user.getId(), superAdmin.getId())) {
            userAdminRoleRepository.save(new UserAdminRole(user, superAdmin));
            log.info("초기 관리자 SUPER_ADMIN 부여: oauthId={}", user.getOauthId());
        }
        user.changeRole(RoleType.ADMIN);
        userRepository.save(user); // 웹 OAuth 로그인처럼 트랜잭션 밖에서 조회된(detached) 사용자도 반영되도록 merge
        adminAuthorizationService.invalidate(user.getId());
    }
}
