package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks private AuthServiceImpl authService;

    @Mock private AuthTokenService authTokenService;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private HttpServletRequest request;
    @Mock private RestClient kakaoRestClient;

    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUpRestClientChain() {
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
    }

    private TokenResponseDto tokens(String access, String refresh) {
        return TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken(access)
                .refreshToken(refresh)
                .accessTokenExpiresIn(3600000L)
                .build();
    }

    // ───────── reissue ─────────

    @Test
    void 토큰_재발급은_RefreshToken만으로_AuthTokenService에_위임() {
        // given: accessToken 필드는 무시되고 refreshToken만 사용된다
        TokenRequestDto requestDto = new TokenRequestDto("ignored.access.token", "valid.refresh.token");
        UUID userId = UUID.randomUUID();
        given(authTokenService.reissue("valid.refresh.token")).willReturn(tokens("new.access", "new.refresh"));
        given(authTokenService.getUserId("new.access")).willReturn(userId);

        // when
        TokenResponseDto result = authService.reissue(requestDto);

        // then
        assertThat(result.accessToken()).isEqualTo("new.access");
        assertThat(result.refreshToken()).isEqualTo("new.refresh");
        // 재발급 감사 로그는 userId를 채워서 발행되어야 함 (audit_logs.user_id NOT NULL)
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.<Object>argThat(event ->
                event instanceof com.jiburo.server.global.log.event.AuditLogEvent e
                        && userId.equals(e.getUserId())
                        && e.getAction() == com.jiburo.server.global.domain.enums.LogActionType.AUTH_REISSUE));
    }

    @Test
    void 유효하지_않은_리프레시_토큰이면_INVALID_REFRESH_TOKEN() {
        // given
        TokenRequestDto requestDto = new TokenRequestDto(null, "invalid.refresh.token");
        given(authTokenService.reissue("invalid.refresh.token"))
                .willThrow(new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN));

        // when & then
        assertThatThrownBy(() -> authService.reissue(requestDto))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    // ───────── logout ─────────

    @Test
    void 로그아웃은_AuthTokenService에_위임() {
        UUID userId = UUID.randomUUID();

        authService.logout(userId, "access.token");

        verify(authTokenService).logout(userId, "access.token");
    }

    // ───────── kakaoLogin ─────────

    private void stubKakaoUserInfo(Map<String, Object> kakaoResponse) {
        given(kakaoRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), any(String[].class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willReturn(kakaoResponse);
    }

    private void stubKakaoUserInfoFailure(RuntimeException exception) {
        given(kakaoRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), any(String[].class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willThrow(exception);
    }

    private Map<String, Object> kakaoUserInfoResponse(long kakaoId, String nickname) {
        return Map.of(
                "id", kakaoId,
                "kakao_account", Map.of(
                        "email", "test@kakao.com",
                        "profile", Map.of(
                                "nickname", nickname,
                                "profile_image_url", "https://kakao.com/profile.png"
                        )
                )
        );
    }

    @Test
    void 카카오_로그인_성공_기존회원() {
        // given
        stubKakaoUserInfo(kakaoUserInfoResponse(12345L, "테스터"));

        User existingUser = User.builder()
                .oauthId("kakao_12345")
                .nickname("테스터")
                .email("test@kakao.com")
                .profileImageUrl("https://kakao.com/profile.png")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(existingUser, "id", UUID.randomUUID());

        given(userRepository.findByOauthId("kakao_12345")).willReturn(Optional.of(existingUser));
        given(authTokenService.issueTokens(existingUser)).willReturn(tokens("access.token", "refresh.token"));

        // when
        TokenResponseDto result = authService.kakaoLogin(new KakaoLoginRequestDto("kakao-access-token"));

        // then
        assertThat(result.accessToken()).isEqualTo("access.token");
        verify(userRepository, never()).save(any());
    }

    @Test
    void 카카오_로그인_성공_신규가입() {
        // given
        stubKakaoUserInfo(kakaoUserInfoResponse(99999L, "신규유저"));

        given(userRepository.findByOauthId("kakao_99999")).willReturn(Optional.empty());

        User savedUser = User.builder()
                .oauthId("kakao_99999")
                .nickname("신규유저")
                .email("test@kakao.com")
                .profileImageUrl("https://kakao.com/profile.png")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(savedUser, "id", UUID.randomUUID());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(authTokenService.issueTokens(savedUser)).willReturn(tokens("new.access.token", "new.refresh.token"));

        // when
        TokenResponseDto result = authService.kakaoLogin(new KakaoLoginRequestDto("kakao-access-token"));

        // then
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 카카오_액세스_토큰이_유효하지_않으면_OAUTH_PROVIDER_FAILED() {
        // given
        stubKakaoUserInfoFailure(new RestClientException("401 Unauthorized"));

        // when & then
        assertThatThrownBy(() -> authService.kakaoLogin(new KakaoLoginRequestDto("invalid-token")))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OAUTH_PROVIDER_FAILED));
    }
}
