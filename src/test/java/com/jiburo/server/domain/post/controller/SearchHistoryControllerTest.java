package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.post.dto.LocationHistoryResponseDto;
import com.jiburo.server.domain.post.service.SearchHistoryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SearchHistoryController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class SearchHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SearchHistoryService searchHistoryService;
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

    // ───────── GET /search/history ─────────

    @Test
    void 키워드_기록_목록_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(get("/api/v1/posts/search/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 키워드_기록_목록_로그인_200_목록_반환() throws Exception {
        // given
        given(searchHistoryService.getKeywords(userId)).willReturn(List.of("강아지", "말티즈"));

        // when & then
        mockMvc.perform(get("/api/v1/posts/search/history").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("강아지"))
                .andExpect(jsonPath("$.data[1]").value("말티즈"));
    }

    @Test
    void 키워드_기록_목록_로그인_결과_없으면_빈_배열() throws Exception {
        // given
        given(searchHistoryService.getKeywords(userId)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/posts/search/history").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ───────── DELETE /search/history ─────────

    @Test
    void 전체_키워드_삭제_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/search/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 전체_키워드_삭제_로그인_200_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/search/history").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(searchHistoryService).should().deleteAllKeywords(eq(userId));
    }

    // ───────── DELETE /search/history/{keyword} ─────────

    @Test
    void 특정_키워드_삭제_로그인_200_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/search/history/{keyword}", "강아지").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(searchHistoryService).should().deleteKeyword(eq(userId), eq("강아지"));
    }

    // ───────── GET /location-history ─────────

    @Test
    void 위치_기록_목록_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(get("/api/v1/posts/location-history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 위치_기록_목록_로그인_200_목록_반환() throws Exception {
        // given
        LocationHistoryResponseDto dto = new LocationHistoryResponseDto(
                37.5665, 126.9780, 3.0, "서울역", "서울 중구 세종대로 1", "37.5665_126.9780_3.0"
        );
        given(searchHistoryService.getLocations(userId)).willReturn(List.of(dto));

        // when & then
        mockMvc.perform(get("/api/v1/posts/location-history").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].lat").value(37.5665))
                .andExpect(jsonPath("$.data[0].lng").value(126.9780))
                .andExpect(jsonPath("$.data[0].radius").value(3.0))
                .andExpect(jsonPath("$.data[0].placeName").value("서울역"))
                .andExpect(jsonPath("$.data[0].address").value("서울 중구 세종대로 1"))
                .andExpect(jsonPath("$.data[0].key").value("37.5665_126.9780_3.0"));
    }

    @Test
    void 위치_기록_목록_placeName_없는_경우_null로_반환() throws Exception {
        // given: placeName, address 없는 위치 기록
        LocationHistoryResponseDto dto = new LocationHistoryResponseDto(
                37.5665, 126.9780, 3.0, null, null, "37.5665_126.9780_3.0"
        );
        given(searchHistoryService.getLocations(userId)).willReturn(List.of(dto));

        // when & then
        mockMvc.perform(get("/api/v1/posts/location-history").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].placeName").doesNotExist())
                .andExpect(jsonPath("$.data[0].address").doesNotExist());
    }

    // ───────── DELETE /location-history ─────────

    @Test
    void 전체_위치_기록_삭제_로그인_200_반환() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/location-history").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(searchHistoryService).should().deleteAllLocations(eq(userId));
    }

    // ───────── DELETE /location-history/{key} ─────────

    @Test
    void 특정_위치_기록_삭제_로그인_200_반환() throws Exception {
        String key = "37.5665_126.9780_3.0";

        mockMvc.perform(delete("/api/v1/posts/location-history/{key}", key).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(searchHistoryService).should().deleteLocation(eq(userId), eq(key));
    }

    // ───────── GET /search/history/enabled ─────────

    @Test
    void 검색기록_설정_조회_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(get("/api/v1/posts/search/history/enabled"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 검색기록_설정_조회_활성_상태_true_반환() throws Exception {
        // given
        given(searchHistoryService.isHistoryEnabled(userId)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/v1/posts/search/history/enabled").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void 검색기록_설정_조회_비활성_상태_false_반환() throws Exception {
        // given
        given(searchHistoryService.isHistoryEnabled(userId)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/v1/posts/search/history/enabled").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    // ───────── POST /search/history/toggle ─────────

    @Test
    void 검색기록_토글_비로그인_UNAUTHORIZED_반환() throws Exception {
        mockMvc.perform(post("/api/v1/posts/search/history/toggle"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 검색기록_토글_활성에서_비활성으로_false_반환() throws Exception {
        // given: 토글 결과 비활성(false) 반환
        given(searchHistoryService.toggleHistoryEnabled(userId)).willReturn(false);

        // when & then
        mockMvc.perform(post("/api/v1/posts/search/history/toggle").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    void 검색기록_토글_비활성에서_활성으로_true_반환() throws Exception {
        // given: 토글 결과 활성(true) 반환
        given(searchHistoryService.toggleHistoryEnabled(userId)).willReturn(true);

        // when & then
        mockMvc.perform(post("/api/v1/posts/search/history/toggle").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
