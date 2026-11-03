package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.AuthTokenRedisRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

/**
 * JWT 발급 / 재발급 / 폐기 담당
 * - 발급 시 Refresh Token을 Redis에 저장 (사용자당 1개: 새 로그인 시 이전 RT는 무효화)
 * - 재발급 시 Redis 저장값과 비교 후 새 RT로 교체 (Rotation)
 * - 로그아웃 시 RT 삭제 + Access Token 블랙리스트 등록
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenRedisRepository authTokenRedisRepository;
    private final UserRepository userRepository;

    public TokenResponseDto issueTokens(User user) {
        CustomOAuth2User principal = new CustomOAuth2User(
                user.getId(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey()))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        TokenResponseDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);
        authTokenRedisRepository.saveRefreshToken(
                user.getId(), tokenDto.refreshToken(), jwtTokenProvider.getRefreshTokenValidityTime()
        );
        return tokenDto;
    }

    public TokenResponseDto reissue(String refreshToken) {
        // 1. 서명/만료/용도(typ=refresh) 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. Redis에 저장된 현재 RT와 일치하는지 확인 (이미 교체/로그아웃된 RT 재사용 차단)
        UUID userId = jwtTokenProvider.getUserId(refreshToken);
        String savedToken = authTokenRedisRepository.findRefreshToken(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!savedToken.equals(refreshToken)) {
            // 교체된 RT가 다시 쓰였다 = 탈취 가능성 -> 해당 사용자 RT를 폐기해 재로그인 유도
            authTokenRedisRepository.deleteRefreshToken(userId);
            throw new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 최신 권한을 반영하기 위해 DB에서 사용자 조회 후 새 토큰 쌍 발급 (RT 교체)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN));
        return issueTokens(user);
    }

    public void logout(UUID userId, String accessToken) {
        authTokenRedisRepository.deleteRefreshToken(userId);
        if (accessToken != null) {
            authTokenRedisRepository.blacklistAccessToken(
                    accessToken, jwtTokenProvider.getRemainingValidityMillis(accessToken)
            );
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return authTokenRedisRepository.isBlacklisted(accessToken);
    }
}
