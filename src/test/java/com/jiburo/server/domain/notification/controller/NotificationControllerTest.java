package com.jiburo.server.domain.notification.controller;

import com.jiburo.server.domain.notification.dto.NotificationResponseDto;
import com.jiburo.server.domain.notification.service.NotificationService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.jwt.JwtAuthenticationEntryPoint;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.jwt.OAuth2SuccessHandler;
import com.jiburo.server.domain.user.service.CustomOAuth2UserService;
import com.jiburo.server.global.config.SecurityConfig;
import com.jiburo.server.global.util.HashidsUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private NotificationService notificationService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    private UUID userId;
    private CustomOAuth2User principal;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        userId = UUID.randomUUID();
        principal = new CustomOAuth2User(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private RequestPostProcessor withUser() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private NotificationResponseDto mockDto() {
        return new NotificationResponseDto(
                HashidsUtils.encode(1L), "NOTI_POST_LIKE", List.of("닉네임"), 10L, false, LocalDateTime.now()
        );
    }

    // ───────── GET /api/v1/notifications ─────────

    @Test
    void 알림_목록_비로그인_401_반환() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 알림_목록_인증_200_반환() throws Exception {
        // given
        given(notificationService.getMyNotifications(eq(userId), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(mockDto()), Pageable.ofSize(20), false));

        // when & then
        mockMvc.perform(get("/api/v1/notifications").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].typeCode").value("NOTI_POST_LIKE"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }

    @Test
    void 알림_목록_빈_결과_200_반환() throws Exception {
        // given
        given(notificationService.getMyNotifications(eq(userId), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), Pageable.ofSize(20), false));

        // when & then
        mockMvc.perform(get("/api/v1/notifications").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ───────── GET /api/v1/notifications/unread-count ─────────

    @Test
    void 안읽은_알림_개수_비로그인_401_반환() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 안읽은_알림_개수_인증_200_반환() throws Exception {
        // given
        given(notificationService.getUnreadCount(userId)).willReturn(3L);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/unread-count").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void 안읽은_알림_없으면_0_반환() throws Exception {
        // given
        given(notificationService.getUnreadCount(userId)).willReturn(0L);

        // when & then
        mockMvc.perform(get("/api/v1/notifications/unread-count").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }
}
