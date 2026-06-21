package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks private AuthServiceImpl authService;

    @Mock private JwtTokenProvider jwtTokenProvider;

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
}
