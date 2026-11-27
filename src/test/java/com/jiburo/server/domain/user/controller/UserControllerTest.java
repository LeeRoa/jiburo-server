package com.jiburo.server.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.domain.finder.service.FinderService;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.jiburo.server.domain.post.service.LostPostService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.UserProfileResponseDto;
import com.jiburo.server.domain.user.dto.UserProfileUpdateRequestDto;
import com.jiburo.server.domain.user.dto.UserSettingsResponseDto;
import com.jiburo.server.domain.user.dto.UserSettingsUpdateRequestDto;
import com.jiburo.server.domain.user.jwt.JwtAccessDeniedHandler;
import com.jiburo.server.domain.user.jwt.JwtAuthenticationEntryPoint;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.jwt.OAuth2SuccessHandler;
import com.jiburo.server.domain.user.service.CustomOAuth2UserService;
import com.jiburo.server.domain.user.service.UserService;
import com.jiburo.server.global.config.SecurityConfig;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private LostPostService lostPostService;
    @MockitoBean private FinderService finderService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private com.jiburo.server.domain.user.service.AuthTokenService authTokenService;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    private UUID userId;
    private CustomOAuth2User principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new CustomOAuth2User(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private RequestPostProcessor withUser() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    // ───────── getMyProfile ─────────

    @Test
    void 프로필_조회_인증_200_반환() throws Exception {
        // given
        given(userService.getMyProfile(userId)).willReturn(
                new UserProfileResponseDto("테스터", "test@test.com", "https://cdn.test/p.png",
                        "google", "BEGINNER", 30, true, true));

        // when & then
        mockMvc.perform(get("/api/v1/users/me").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("테스터"))
                .andExpect(jsonPath("$.data.provider").value("google"))
                .andExpect(jsonPath("$.data.activityScore").value(30));
    }

    @Test
    void 프로필_조회_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // ───────── updateMyProfile ─────────

    @Test
    void 프로필_수정_인증_200_반환() throws Exception {
        // given
        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("새닉네임", null);
        given(userService.updateMyProfile(eq(userId), any())).willReturn(
                new UserProfileResponseDto("새닉네임", "test@test.com", null,
                        "google", "BEGINNER", 30, true, true));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
    }

    @Test
    void 닉네임_중복이면_409_반환() throws Exception {
        // given
        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("중복닉", null);
        willThrow(new JiburoException(ErrorCode.DUPLICATE_NICKNAME))
                .given(userService).updateMyProfile(eq(userId), any());

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"));
    }

    @Test
    void 닉네임이_두_글자_미만이면_400_반환() throws Exception {
        // given
        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("가", null);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ───────── settings ─────────

    @Test
    void 설정_변경_인증_200_반환() throws Exception {
        // given
        UserSettingsUpdateRequestDto request = new UserSettingsUpdateRequestDto(false, null);
        given(userService.updateMySettings(eq(userId), any()))
                .willReturn(new UserSettingsResponseDto(false, true));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationEnabled").value(false))
                .andExpect(jsonPath("$.data.locationEnabled").value(true));
    }

    // ───────── getMyPosts ─────────

    @Test
    void 내_게시물_조회_시_mineOnly가_true로_강제된다() throws Exception {
        // given
        given(lostPostService.search(any(), any(Pageable.class), eq(userId)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        mockMvc.perform(get("/api/v1/users/me/posts")
                        .param("mineOnly", "false")
                        .param("statusCode", "LOST")
                        .with(withUser()))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<LostPostSearchCondition> captor = ArgumentCaptor.forClass(LostPostSearchCondition.class);
        then(lostPostService).should().search(captor.capture(), any(Pageable.class), eq(userId));
        assertThat(captor.getValue().mineOnly()).isTrue();
        assertThat(captor.getValue().statusCode()).isEqualTo(PostStatus.LOST);
    }

    // ───────── withdraw ─────────

    @Test
    void 회원탈퇴_인증_200_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(userService).should().withdraw(userId);
    }

    @Test
    void 회원탈퇴_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
