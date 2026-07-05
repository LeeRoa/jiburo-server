package com.jiburo.server.domain.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.domain.post.domain.enums.CategoryType;
import com.jiburo.server.domain.post.domain.enums.PostStatus;
import com.jiburo.server.domain.post.dto.LostPostResponseDto;
import com.jiburo.server.domain.finder.service.FinderService;
import com.jiburo.server.domain.post.service.LostPostService;
import com.jiburo.server.domain.post.service.SearchHistoryService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.jwt.JwtAuthenticationEntryPoint;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.jwt.OAuth2SuccessHandler;
import com.jiburo.server.domain.user.service.CustomOAuth2UserService;
import com.jiburo.server.global.config.SecurityConfig;
import com.jiburo.server.global.util.HashidsUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LostPostController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class LostPostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private LostPostService lostPostService;
    @MockitoBean private SearchHistoryService searchHistoryService;
    @MockitoBean private FinderService finderService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;

    private UUID userId;
    private CustomOAuth2User principal;
    private String encodedId;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
        userId = UUID.randomUUID();
        principal = new CustomOAuth2User(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        encodedId = HashidsUtils.encode(1L);
    }

    // 인증된 사용자 SecurityContext 주입 헬퍼
    private RequestPostProcessor withUser() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private LostPostResponseDto mockDto(boolean isLiked, long likeCount) {
        return new LostPostResponseDto(
                encodedId, userId, "작성자", "제목", "내용",
                PostStatus.LOST, CategoryType.ANIMAL, null,
                "http://test.com/img.jpg", 37.5, 127.0, "서울 강남구",
                LocalDate.of(2024, 1, 1), 10000, LocalDateTime.now(),
                isLiked, likeCount
        );
    }

    // ───────── POST /{id}/like ─────────

    @Test
    void 좋아요_토글_비로그인_UNAUTHORIZED_반환() throws Exception {
        // SecurityConfig에서 POST /api/v1/posts/** 는 anyRequest().authenticated()
        // → JwtAuthenticationEntryPoint가 401 응답
        mockMvc.perform(post("/api/v1/posts/{id}/like", encodedId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 좋아요_토글_인증_좋아요_추가_true_반환() throws Exception {
        // given
        given(lostPostService.toggleLike(eq(userId), any())).willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/posts/{id}/like", encodedId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void 좋아요_토글_인증_좋아요_취소_false_반환() throws Exception {
        // given
        given(lostPostService.toggleLike(eq(userId), any())).willReturn(false);

        // when & then
        mockMvc.perform(post("/api/v1/posts/{id}/like", encodedId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    // ───────── GET /liked ─────────

    @Test
    void 관심_게시글_목록_비로그인_UNAUTHORIZED_반환() throws Exception {
        // GET /api/v1/posts/** 는 permitAll이지만 컨트롤러에서 user == null 이면 UNAUTHORIZED throw
        mockMvc.perform(get("/api/v1/posts/liked"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 관심_게시글_목록_인증_200_반환() throws Exception {
        // given
        LostPostResponseDto dto = mockDto(true, 3L);
        Page<LostPostResponseDto> page = new PageImpl<>(List.of(dto));
        given(lostPostService.getLikedPosts(eq(userId), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/posts/liked")
                        .with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].isLiked").value(true))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(3));
    }

    // ───────── GET /{id} ─────────

    @Test
    void 단건_조회_비로그인_isLiked_false() throws Exception {
        // given
        LostPostResponseDto dto = mockDto(false, 5L);
        given(lostPostService.findById(any(), eq(null))).willReturn(dto);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{id}", encodedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(5));
    }

    @Test
    void 단건_조회_로그인_isLiked_정보_포함() throws Exception {
        // given
        LostPostResponseDto dto = mockDto(true, 12L);
        given(lostPostService.findById(any(), eq(userId))).willReturn(dto);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{id}", encodedId)
                        .with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(12));
    }

    // ───────── GET /search ─────────

    @Test
    void 목록_검색_비로그인_isLiked_false() throws Exception {
        // given
        LostPostResponseDto dto = mockDto(false, 2L);
        Page<LostPostResponseDto> page = new PageImpl<>(List.of(dto));
        given(lostPostService.search(any(), any(), eq(null))).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/posts/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].isLiked").value(false));
    }

    // ───────── GET /nearby ─────────

    @Test
    void 반경_조회_sortType_DISTANCE_200_반환() throws Exception {
        // given
        LostPostResponseDto dto = mockDto(false, 0L);
        given(lostPostService.getPostsForList(any()))
                .willReturn(new SliceImpl<>(List.of(dto), PageRequest.of(0, 20), false));

        // when & then
        mockMvc.perform(get("/api/v1/posts/nearby")
                        .param("centerLat", "37.5")
                        .param("centerLng", "127.0")
                        .param("radius", "3.0")
                        .param("sortType", "DISTANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(encodedId));
    }

    @Test
    void 반경_조회_sortType_LATEST_200_반환() throws Exception {
        // given
        LostPostResponseDto dto = mockDto(false, 0L);
        given(lostPostService.getPostsForList(any()))
                .willReturn(new SliceImpl<>(List.of(dto), PageRequest.of(0, 20), false));

        // when & then
        mockMvc.perform(get("/api/v1/posts/nearby")
                        .param("centerLat", "37.5")
                        .param("centerLng", "127.0")
                        .param("radius", "3.0")
                        .param("sortType", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(encodedId));
    }

    @Test
    void 반경_조회_sortType_생략시_200_반환() throws Exception {
        // given: sortType 미전달 → DISTANCE 기본값으로 처리
        LostPostResponseDto dto = mockDto(false, 0L);
        given(lostPostService.getPostsForList(any()))
                .willReturn(new SliceImpl<>(List.of(dto), PageRequest.of(0, 20), false));

        // when & then
        mockMvc.perform(get("/api/v1/posts/nearby")
                        .param("centerLat", "37.5")
                        .param("centerLng", "127.0")
                        .param("radius", "3.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
