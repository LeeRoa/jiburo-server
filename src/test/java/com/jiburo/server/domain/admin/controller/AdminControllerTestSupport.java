package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.jwt.JwtAccessDeniedHandler;
import com.jiburo.server.domain.user.jwt.JwtAuthenticationEntryPoint;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.jwt.OAuth2SuccessHandler;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.domain.user.service.CustomOAuth2UserService;
import com.jiburo.server.global.config.SecurityConfig;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

/**
 * 관리자 컨트롤러 WebMvc 테스트 공통 설정.
 * 하위 클래스는 @WebMvcTest(controllers = Xxx.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
 * 만 붙이고 테스트할 서비스를 @MockitoBean으로 선언하면 된다.
 */
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        com.jiburo.server.domain.admin.service.AdminAccessEvaluator.class})
public abstract class AdminControllerTestSupport {

    @Autowired protected MockMvc mockMvc;

    // 메뉴 권한 판단 (기본: 아무 권한 없음). withAdmin()은 SUPER_ADMIN처럼 전부 허용,
    // withAdmin(menu, level)은 해당 메뉴 권한만 허용한다.
    @MockitoBean protected com.jiburo.server.domain.admin.service.AdminAuthorizationService adminAuthorizationService;

    @MockitoBean protected JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean protected JwtTokenProvider jwtTokenProvider;
    @MockitoBean protected AuthTokenService authTokenService;
    @MockitoBean protected CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean protected OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean protected ClientRegistrationRepository clientRegistrationRepository;

    protected UUID adminId;
    protected UUID userId;

    @BeforeEach
    void setUpPrincipals() {
        new HashidsUtils("test-salt", 8);
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    // 모든 메뉴 편집 가능한 관리자 (SUPER_ADMIN)
    protected RequestPostProcessor withAdmin() {
        org.mockito.BDDMockito.given(adminAuthorizationService.can(
                org.mockito.ArgumentMatchers.eq(adminId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(true);
        org.mockito.BDDMockito.given(adminAuthorizationService.hasAnyRole(adminId)).willReturn(true);
        return as(adminId, "ROLE_ADMIN");
    }

    // 특정 메뉴 권한만 가진 관리자 (WRITE는 READ 포함)
    protected RequestPostProcessor withAdmin(com.jiburo.server.domain.admin.domain.AdminMenu menu,
                                             com.jiburo.server.domain.admin.domain.AdminAccessLevel level) {
        org.mockito.BDDMockito.given(adminAuthorizationService.hasAnyRole(adminId)).willReturn(true);
        for (com.jiburo.server.domain.admin.domain.AdminAccessLevel required
                : com.jiburo.server.domain.admin.domain.AdminAccessLevel.values()) {
            org.mockito.BDDMockito.given(adminAuthorizationService.can(adminId, menu, required))
                    .willReturn(level.covers(required));
        }
        return as(adminId, "ROLE_ADMIN");
    }

    // ROLE_ADMIN 토큰만 있고 메뉴 권한은 없는 상태 (역할이 회수됐지만 토큰이 아직 유효한 경우)
    protected RequestPostProcessor withAdminTokenOnly() {
        return as(adminId, "ROLE_ADMIN");
    }

    protected RequestPostProcessor withUser() {
        return as(userId, "ROLE_USER");
    }

    private RequestPostProcessor as(UUID id, String role) {
        CustomOAuth2User principal = new CustomOAuth2User(id, List.of(new SimpleGrantedAuthority(role)));
        return SecurityMockMvcRequestPostProcessors.authentication(
                new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
