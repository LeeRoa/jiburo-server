package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
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
import org.springframework.security.core.Authentication;
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

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private HttpServletRequest request;
    @Mock private RestClient.Builder restClientBuilder;

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUpRestClientChain() {
        restClient = mock(RestClient.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
    }

    // ───────── reissue ─────────

    @Test
    void 토큰_재발급_성공() {
        // given
        TokenRequestDto request = new TokenRequestDto("valid.access.token", "valid.refresh.token");

        Authentication authentication = mock(Authentication.class);
        TokenResponseDto newTokens = TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .accessTokenExpiresIn(3600000L)
                .build();

        given(jwtTokenProvider.validateToken("valid.refresh.token")).willReturn(true);
        given(jwtTokenProvider.getAuthentication("valid.access.token")).willReturn(authentication);
        given(jwtTokenProvider.generateTokenDto(authentication)).willReturn(newTokens);

        // when
        TokenResponseDto result = authService.reissue(request);

        // then
        assertThat(result.grantType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        assertThat(result.refreshToken()).isEqualTo("new.refresh.token");
        assertThat(result.accessTokenExpiresIn()).isEqualTo(3600000L);
    }

    @Test
    void 유효하지_않은_리프레시_토큰이면_INVALID_TOKEN() {
        // given
        TokenRequestDto request = new TokenRequestDto("expired.access.token", "invalid.refresh.token");
        given(jwtTokenProvider.validateToken("invalid.refresh.token")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(request))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    // ───────── kakaoLogin ─────────

    private void stubKakaoUserInfo(Map<String, Object> kakaoResponse) {
        given(restClientBuilder.build()).willReturn(restClient);
        given(restClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), any(String[].class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(any(ParameterizedTypeReference.class))).willReturn(kakaoResponse);
    }

    private void stubKakaoUserInfoFailure(RuntimeException exception) {
        given(restClientBuilder.build()).willReturn(restClient);
        given(restClient.get()).willReturn(requestHeadersUriSpec);
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

        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .oauthId("kakao_12345")
                .nickname("테스터")
                .email("test@kakao.com")
                .profileImageUrl("https://kakao.com/profile.png")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(existingUser, "id", userId);

        given(userRepository.findByOauthId("kakao_12345")).willReturn(Optional.of(existingUser));

        TokenResponseDto tokens = TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken("access.token")
                .refreshToken("refresh.token")
                .accessTokenExpiresIn(3600000L)
                .build();
        given(jwtTokenProvider.generateTokenDto(any(Authentication.class))).willReturn(tokens);

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

        TokenResponseDto tokens = TokenResponseDto.builder()
                .grantType("Bearer")
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .accessTokenExpiresIn(3600000L)
                .build();
        given(jwtTokenProvider.generateTokenDto(any(Authentication.class))).willReturn(tokens);

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
