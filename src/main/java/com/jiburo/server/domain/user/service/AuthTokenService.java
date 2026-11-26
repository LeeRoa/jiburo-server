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
import java.util.Optional;
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

    // 토큰 쌍 생성 (Redis 저장은 호출 측: 로그인 = 저장, 재발급 = 원자적 교체)
    private IssuedTokens createTokens(User user) {
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
        return new IssuedTokens(jwtTokenProvider.generateTokenDto(authentication, accessTtl, refreshTtl()), refreshTtl());
    }

    private record IssuedTokens(TokenResponseDto dto, long refreshTtl) {}

    public TokenResponseDto issueTokens(User user) {
        IssuedTokens issued = createTokens(user);
        authTokenRedisRepository.saveRefreshToken(user.getId(), issued.dto().refreshToken(), issued.refreshTtl());
        return issued.dto();
    }

    private long refreshTtl() {
        return Duration.ofDays(appSettingService.getInt(SettingKey.REFRESH_TOKEN_TTL_DAYS)).toMillis();
    }

    public TokenResponseDto reissue(String refreshToken) {
        // 1. 서명/만료/용도(typ=refresh) 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 최신 권한·정지 여부를 반영하기 위해 DB에서 사용자 조회 후 새 토큰 쌍 생성 (정지 계정은 여기서 거부)
        UUID userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN));
        IssuedTokens issued = createTokens(user);

        // 3. Redis의 현재 RT와 비교·교체를 원자적으로 (같은 RT로 동시에 재발급해도 하나만 성공)
        return switch (authTokenRedisRepository.rotateRefreshToken(
                userId, refreshToken, issued.dto().refreshToken(), issued.refreshTtl())) {
            case ROTATED -> issued.dto();
            // 로그아웃·정지로 RT가 없음, 또는 이미 교체된 RT가 다시 쓰임(탈취 의심 → 저장 RT는 스크립트가 폐기)
            case MISSING, MISMATCH -> throw new JiburoException(ErrorCode.INVALID_REFRESH_TOKEN);
        };
    }

    /**
     * 사용자의 모든 토큰을 즉시 무효화 (계정 정지).
     * Refresh Token을 지워 재발급을 끊고, 지금까지 발급된 Access Token도 남은 유효 시간과 상관없이 거부되게 한다.
     */
    public void revokeAllTokens(UUID userId) {
        authTokenRedisRepository.deleteRefreshToken(userId);
        // 표시는 Access Token이 가질 수 있는 최대 유효 시간 동안만 유지하면 된다 (그 뒤엔 전부 만료)
        long maxAccessTtl = Math.max(
                Duration.ofMinutes(SettingKey.ACCESS_TOKEN_TTL_MINUTES.getMax().longValue()).toMillis(),
                jwtTokenProvider.getAccessTokenValidityTime());
        authTokenRedisRepository.revokeTokensIssuedBefore(userId, System.currentTimeMillis(), maxAccessTtl + Duration.ofMinutes(5).toMillis());
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

    /**
     * 서명·만료는 유효하지만 쓰면 안 되는 Access Token인지 — 로그아웃(블랙리스트)됐거나,
     * 계정 정지 등으로 사용자 토큰이 일괄 무효화된 시각 이전에 발급됐으면 true.
     * 무효한 토큰이면 true (호출 측은 validateAccessToken을 먼저 확인한다).
     */
    public boolean isRevoked(String accessToken) {
        Optional<Long> issuedAt = jwtTokenProvider.getIssuedAtMillis(accessToken);
        if (issuedAt.isEmpty()) {
            return true;
        }
        AuthTokenRedisRepository.Revocation revocation =
                authTokenRedisRepository.findRevocation(accessToken, jwtTokenProvider.getUserId(accessToken));
        if (revocation.blacklisted()) {
            return true;
        }
        // iat는 초 단위(내림)라 무효화와 같은 초에 발급된 토큰도 무효로 본다 (정지 이후엔 발급 자체가 막혀 있음)
        return revocation.revokedBefore() != null && issuedAt.get() <= revocation.revokedBefore();
    }
}
