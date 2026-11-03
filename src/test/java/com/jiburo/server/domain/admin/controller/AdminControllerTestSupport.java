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
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public abstract class AdminControllerTestSupport {

    @Autowired protected MockMvc mockMvc;

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

    protected RequestPostProcessor withAdmin() {
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
