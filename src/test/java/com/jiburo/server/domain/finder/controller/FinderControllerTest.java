package com.jiburo.server.domain.finder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.domain.finder.domain.enums.FinderResultType;
import com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus;
import com.jiburo.server.domain.finder.dto.*;
import com.jiburo.server.domain.finder.service.FinderService;
import com.jiburo.server.domain.image.domain.UploadTargetType;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FinderController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class
FinderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private FinderService finderService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    private UUID userId;
    private CustomOAuth2User principal;
    private String encodedSessionId;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        userId = UUID.randomUUID();
        principal = new CustomOAuth2User(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        encodedSessionId = HashidsUtils.encode(10L);
    }

    private RequestPostProcessor withUser() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private FinderSessionResponseDto mockSessionResponse() {
        return new FinderSessionResponseDto(
                encodedSessionId, HashidsUtils.encode(1L), "분실 강아지 찾아요",
                FinderSessionStatus.ACTIVE.name(), 37.5, 127.0,
                null, null, null, List.of(), LocalDateTime.now()
        );
    }

    // ───────── POST / (세션 생성) ─────────

    @Test
    void 세션_생성_비로그인_UNAUTHORIZED() throws Exception {
        mockMvc.perform(post("/api/v1/finder/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinderSessionCreateRequestDto(HashidsUtils.encode(1L), 37.5, 127.0))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 세션_생성_성공_201() throws Exception {
        given(finderService.createSession(eq(userId), any())).willReturn(encodedSessionId);

        mockMvc.perform(post("/api/v1/finder/sessions")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FinderSessionCreateRequestDto(HashidsUtils.encode(1L), 37.5, 127.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(encodedSessionId));
    }

    // ───────── GET / (내 세션 목록) ─────────

    @Test
    void 내_세션_목록_비로그인_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get("/api/v1/finder/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 내_세션_목록_조회_성공() throws Exception {
        var listDto = new FinderSessionListResponseDto(
                encodedSessionId, HashidsUtils.encode(1L), "분실 강아지 찾아요", null,
                FinderSessionStatus.ACTIVE.name(), 37.5, 127.0, null, 0, LocalDateTime.now()
        );
        given(finderService.getMySessions(eq(userId), any()))
                .willReturn(new SliceImpl<>(List.of(listDto), PageRequest.of(0, 10), false));

        mockMvc.perform(get("/api/v1/finder/sessions").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(encodedSessionId));
    }

    // ───────── GET /{sessionId} (세션 상세) ─────────

    @Test
    void 세션_상세_조회_성공() throws Exception {
        given(finderService.getSession(eq(userId), eq(10L))).willReturn(mockSessionResponse());

        mockMvc.perform(get("/api/v1/finder/sessions/{id}", encodedSessionId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(encodedSessionId))
                .andExpect(jsonPath("$.data.statusCode").value("ACTIVE"));
    }

    // ───────── POST /{sessionId}/media (미디어 추가) ─────────

    @Test
    void 미디어_추가_파일_성공() throws Exception {
        var mediaDto = new FinderMediaResponseDto(
                HashidsUtils.encode(100L), "finder/images/test.jpg",
                UploadTargetType.FINDER_IMAGE.name(), 37.5, 127.0, null, LocalDateTime.now()
        );
        given(finderService.addMedia(eq(userId), eq(10L), any())).willReturn(List.of(mediaDto));

        var request = new FinderMediaAddRequestDto(
                List.of("finder/images/test.jpg"), UploadTargetType.FINDER_IMAGE, 37.5, 127.0, null
        );

        mockMvc.perform(post("/api/v1/finder/sessions/{id}/media", encodedSessionId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].fileKey").value("finder/images/test.jpg"))
                .andExpect(jsonPath("$.data[0].mediaType").value("FINDER_IMAGE"));
    }

    @Test
    void 미디어_추가_텍스트_성공() throws Exception {
        var mediaDto = new FinderMediaResponseDto(
                HashidsUtils.encode(101L), null, null, null, null, "한강공원에서 목격", LocalDateTime.now()
        );
        given(finderService.addMedia(eq(userId), eq(10L), any())).willReturn(List.of(mediaDto));

        var request = new FinderMediaAddRequestDto(null, null, null, null, "한강공원에서 목격");

        mockMvc.perform(post("/api/v1/finder/sessions/{id}/media", encodedSessionId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memo").value("한강공원에서 목격"));
    }

    // ───────── POST /{sessionId}/submit (결과 제출) ─────────

    @Test
    void 결과_제출_FOUND_성공() throws Exception {
        willDoNothing().given(finderService).submitSession(eq(userId), eq(10L), any());

        var request = new FinderSessionSubmitRequestDto(FinderResultType.FOUND, "강아지 발견!");

        mockMvc.perform(post("/api/v1/finder/sessions/{id}/submit", encodedSessionId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ───────── POST /{sessionId}/route (경로 기록) ─────────

    @Test
    void 경로_기록_성공() throws Exception {
        willDoNothing().given(finderService).recordRoute(eq(userId), eq(10L), any());

        var request = new FinderRouteRecordRequestDto(List.of(
                new FinderRouteRecordRequestDto.RoutePoint(37.5665, 126.9780, LocalDateTime.of(2026, 7, 5, 10, 0, 0)),
                new FinderRouteRecordRequestDto.RoutePoint(37.5670, 126.9785, LocalDateTime.of(2026, 7, 5, 10, 0, 10))
        ));

        mockMvc.perform(post("/api/v1/finder/sessions/{id}/route", encodedSessionId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 경로_기록_빈_배열이면_400() throws Exception {
        var request = new FinderRouteRecordRequestDto(List.of());

        mockMvc.perform(post("/api/v1/finder/sessions/{id}/route", encodedSessionId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ───────── GET /{sessionId}/route (경로 조회) ─────────

    @Test
    void 경로_조회_성공() throws Exception {
        var routePoints = List.of(
                new FinderRoutePointResponseDto(37.5665, 126.9780, LocalDateTime.of(2026, 7, 5, 10, 0, 0)),
                new FinderRoutePointResponseDto(37.5670, 126.9785, LocalDateTime.of(2026, 7, 5, 10, 0, 10))
        );
        given(finderService.getRoute(eq(userId), eq(10L))).willReturn(routePoints);

        mockMvc.perform(get("/api/v1/finder/sessions/{id}/route", encodedSessionId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].latitude").value(37.5665))
                .andExpect(jsonPath("$.data[1].latitude").value(37.5670));
    }

    // ───────── DELETE /{sessionId} (세션 취소) ─────────

    @Test
    void 세션_취소_성공() throws Exception {
        willDoNothing().given(finderService).cancelSession(eq(userId), eq(10L));

        mockMvc.perform(delete("/api/v1/finder/sessions/{id}", encodedSessionId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
