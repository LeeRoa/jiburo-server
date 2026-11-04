package com.jiburo.server.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.domain.admin.service.AdminBootstrapService;
import com.jiburo.server.domain.user.dao.AuthTokenRedisRepository;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.setting.AppSettingService;
import com.jiburo.server.global.setting.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
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

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int LOGIN_CODE_BYTES = 32; // 256bit — 추측 불가

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenRedisRepository authTokenRedisRepository;
    private final UserRepository userRepository;
    private final AdminBootstrapService adminBootstrapService;
    // 토큰 유효 시간은 운영 설정(ACCESS_TOKEN_TTL_MINUTES / REFRESH_TOKEN_TTL_DAYS / LOGIN_CODE_TTL_SECONDS).
    // 변경하면 이후 새로 발급되는 토큰부터 적용된다.
    private final AppSettingService appSettingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 로그인 시점 토큰 발급. 초기 관리자(ADMIN_OAUTH_IDS) 계정이면 SUPER_ADMIN을 먼저 부여한 뒤 발급한다.
     * (재발급은 issueTokens를 쓴다 — 로그인 때 이미 반영되어 있음)
     */
    public TokenResponseDto issueLoginTokens(User user) {
        adminBootstrapService.applyOnLogin(user);
        return issueTokens(user);
    }

    public TokenResponseDto issueTokens(User user) {
        // 로그인·재발급 모두 여기를 지나므로 정지 계정은 한 곳에서 막는다
        if (user.isSuspended()) {
            throw new JiburoException(ErrorCode.USER_SUSPENDED);
        }
        CustomOAuth2User principal = new CustomOAuth2User(
                user.getId(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey()))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        long accessTtl = Duration.ofMinutes(appSettingService.getInt(SettingKey.ACCESS_TOKEN_TTL_MINUTES)).toMillis();
        long refreshTtl = Duration.ofDays(appSettingService.getInt(SettingKey.REFRESH_TOKEN_TTL_DAYS)).toMillis();

        TokenResponseDto tokenDto = jwtTokenProvider.generateTokenDto(authentication, accessTtl, refreshTtl);
        authTokenRedisRepository.saveRefreshToken(user.getId(), tokenDto.refreshToken(), refreshTtl);
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

    /**
     * 웹 OAuth 로그인 후 redirect URL에 실을 1회용 code 발급.
     * 토큰 자체는 Redis에만 두고(TTL 짧음), 프론트가 code를 POST로 보내 교환한다.
     */
    public String createLoginCode(TokenResponseDto tokenDto) {
        byte[] bytes = new byte[LOGIN_CODE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            authTokenRedisRepository.saveLoginCode(code, objectMapper.writeValueAsString(tokenDto),
                    Duration.ofSeconds(appSettingService.getInt(SettingKey.LOGIN_CODE_TTL_SECONDS)).toMillis());
        } catch (JsonProcessingException e) {
            throw new JiburoException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return code;
    }

    // 1회용 code -> 토큰. 한 번 교환하면 즉시 삭제되고, 만료/재사용/위조 code는 INVALID_AUTH_CODE
    public TokenResponseDto exchangeLoginCode(String code) {
        String tokenJson = authTokenRedisRepository.consumeLoginCode(code)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_AUTH_CODE));
        try {
            return objectMapper.readValue(tokenJson, TokenResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new JiburoException(ErrorCode.INVALID_AUTH_CODE);
        }
    }

    // 유효한 토큰의 sub(userId) — 감사 로그 등에서 사용
    public UUID getUserId(String token) {
        return jwtTokenProvider.getUserId(token);
    }

    public boolean isBlacklisted(String accessToken) {
        return authTokenRedisRepository.isBlacklisted(accessToken);
    }
}
