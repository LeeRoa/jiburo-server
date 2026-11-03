package com.jiburo.server.domain.user.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * 인증 토큰 관련 Redis 저장소
 * - auth:refresh:{userId}       : 현재 유효한 Refresh Token (TTL = RT 만료시간)
 * - auth:blacklist:{sha256(AT)} : 로그아웃된 Access Token (TTL = AT 남은 유효시간)
 * - auth:login-code:{sha256(code)} : 웹 OAuth 로그인 1회용 code -> 토큰 JSON (TTL 짧음, 조회 시 삭제)
 */
@Repository
@RequiredArgsConstructor
public class AuthTokenRedisRepository {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String LOGIN_CODE_PREFIX = "auth:login-code:";

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(UUID userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, refreshToken, Duration.ofMillis(ttlMillis));
    }

    public Optional<String> findRefreshToken(UUID userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_PREFIX + userId));
    }

    public void deleteRefreshToken(UUID userId) {
        redisTemplate.delete(REFRESH_PREFIX + userId);
    }

    public void blacklistAccessToken(String accessToken, long ttlMillis) {
        if (ttlMillis <= 0) return; // 이미 만료된 토큰은 저장할 필요 없음
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + hash(accessToken), "logout", Duration.ofMillis(ttlMillis));
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + hash(accessToken)));
    }

    // 웹 OAuth 로그인 1회용 code -> 발급된 토큰(JSON). 짧은 TTL로 저장
    public void saveLoginCode(String code, String tokenJson, long ttlMillis) {
        redisTemplate.opsForValue().set(LOGIN_CODE_PREFIX + hash(code), tokenJson, Duration.ofMillis(ttlMillis));
    }

    // 조회와 동시에 삭제 (GETDEL) — 같은 code로 두 번 교환할 수 없다
    public Optional<String> consumeLoginCode(String code) {
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(LOGIN_CODE_PREFIX + hash(code)));
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
