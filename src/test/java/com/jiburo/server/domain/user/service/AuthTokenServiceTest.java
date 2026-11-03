package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.AuthTokenRedisRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 실제 JwtTokenProvider(서명/만료/typ 검증)와 Mock Redis 저장소를 조합해 토큰 수명주기를 검증한다.
 */
class AuthTokenServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("jiburo-test-secret-key-must-be-32-bytes-or-more!!".getBytes());

    private JwtTokenProvider jwtTokenProvider;
    private AuthTokenRedisRepository redisRepository;
    private UserRepository userRepository;
    private AuthTokenService authTokenService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 1_800_000L, 1_209_600_000L);
        redisRepository = mock(AuthTokenRedisRepository.class);
        userRepository = mock(UserRepository.class);
        authTokenService = new AuthTokenService(jwtTokenProvider, redisRepository, userRepository);

        userId = UUID.randomUUID();
        user = User.builder()
                .oauthId("kakao_1")
                .nickname("tester")
                .email("t@t.com")
                .profileImageUrl("img")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    @Test
    void 발급된_토큰은_typ으로_구분되고_RT는_Redis에_저장된다() {
        TokenResponseDto tokens = authTokenService.issueTokens(user);

        assertThat(jwtTokenProvider.validateAccessToken(tokens.accessToken())).isTrue();
        assertThat(jwtTokenProvider.validateRefreshToken(tokens.accessToken())).isFalse();
        assertThat(jwtTokenProvider.validateRefreshToken(tokens.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.validateAccessToken(tokens.refreshToken())).isFalse();
        assertThat(jwtTokenProvider.getUserId(tokens.refreshToken())).isEqualTo(userId);
        verify(redisRepository).saveRefreshToken(eq(userId), eq(tokens.refreshToken()), anyLong());
    }

    @Test
    void RefreshToken을_AccessToken으로_쓰면_인증_정보를_만들_수_없다() {
        TokenResponseDto tokens = authTokenService.issueTokens(user);

        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tokens.refreshToken()))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    void 재발급_성공시_새_RT로_교체된다() {
        TokenResponseDto first = authTokenService.issueTokens(user);
        given(redisRepository.findRefreshToken(userId)).willReturn(Optional.of(first.refreshToken()));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        TokenResponseDto second = authTokenService.reissue(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        verify(redisRepository).saveRefreshToken(eq(userId), eq(second.refreshToken()), anyLong());
    }

    @Test
    void Redis에_저장된_RT와_다르면_RT를_폐기하고_INVALID_REFRESH_TOKEN() {
        TokenResponseDto stolen = authTokenService.issueTokens(user);
        given(redisRepository.findRefreshToken(userId)).willReturn(Optional.of("rotated.newer.token"));

        assertThatThrownBy(() -> authTokenService.reissue(stolen.refreshToken()))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        verify(redisRepository).deleteRefreshToken(userId);
    }

    @Test
    void 로그아웃된_사용자의_RT로는_재발급_불가() {
        TokenResponseDto tokens = authTokenService.issueTokens(user);
        given(redisRepository.findRefreshToken(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authTokenService.reissue(tokens.refreshToken()))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    void AccessToken으로_재발급을_시도하면_INVALID_REFRESH_TOKEN() {
        TokenResponseDto tokens = authTokenService.issueTokens(user);

        assertThatThrownBy(() -> authTokenService.reissue(tokens.accessToken()))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        verify(redisRepository, never()).findRefreshToken(userId);
    }

    @Test
    void 로그아웃시_RT_삭제_및_AT_블랙리스트_등록() {
        TokenResponseDto tokens = authTokenService.issueTokens(user);

        authTokenService.logout(userId, tokens.accessToken());

        verify(redisRepository).deleteRefreshToken(userId);
        verify(redisRepository).blacklistAccessToken(eq(tokens.accessToken()), anyLong());
    }

    @Test
    void 로그인_code는_추측_불가한_값이고_토큰은_Redis에만_저장된다() {
        TokenResponseDto tokens = authTokenService.issueTokens(user);

        String code = authTokenService.createLoginCode(tokens);

        assertThat(code).hasSizeGreaterThanOrEqualTo(43) // 32바이트 base64url
                .doesNotContain(tokens.accessToken())
                .doesNotContain(tokens.refreshToken());
        verify(redisRepository).saveLoginCode(eq(code), org.mockito.ArgumentMatchers.contains(tokens.accessToken()), anyLong());
    }

    @Test
    void 로그인_code를_토큰으로_교환한다() throws Exception {
        TokenResponseDto tokens = authTokenService.issueTokens(user);
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(tokens);
        given(redisRepository.consumeLoginCode("the-code")).willReturn(Optional.of(json));

        TokenResponseDto result = authTokenService.exchangeLoginCode("the-code");

        assertThat(result.accessToken()).isEqualTo(tokens.accessToken());
        assertThat(result.refreshToken()).isEqualTo(tokens.refreshToken());
    }

    @Test
    void 만료되었거나_이미_사용된_code는_INVALID_AUTH_CODE() {
        given(redisRepository.consumeLoginCode("used-code")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authTokenService.exchangeLoginCode("used-code"))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_AUTH_CODE));
    }

    @Test
    void 로그아웃시_AT가_없으면_블랙리스트_등록은_생략() {
        authTokenService.logout(userId, null);

        verify(redisRepository).deleteRefreshToken(userId);
        verify(redisRepository, never()).blacklistAccessToken(anyString(), anyLong());
    }
}
