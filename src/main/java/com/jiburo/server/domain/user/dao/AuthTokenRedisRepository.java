package com.jiburo.server.domain.user.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 인증 토큰 관련 Redis 저장소
 * - auth:refresh:{userId}       : 현재 유효한 Refresh Token (TTL = RT 만료시간)
 * - auth:blacklist:{sha256(AT)} : 로그아웃된 Access Token (TTL = AT 남은 유효시간)
 * - auth:login-code:{sha256(code)} : 웹 OAuth 로그인 1회용 code -> 토큰 JSON (TTL 짧음, 조회 시 삭제)
 * - auth:revoked-before:{userId}   : 이 시각(epoch ms) 이전 발급 Access Token 일괄 무효 (계정 정지)
 */
@Repository
@RequiredArgsConstructor
public class AuthTokenRedisRepository {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String LOGIN_CODE_PREFIX = "auth:login-code:";
    private static final String REVOKED_BEFORE_PREFIX = "auth:revoked-before:";

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

    public enum RotationResult { ROTATED, MISSING, MISMATCH }

    /**
     * Refresh Token 교체(Rotation)를 원자적으로 수행한다 (비교 + 교체를 한 번에 — 같은 RT로 동시에 재발급해도 하나만 성공).
     * - 저장값 == expected → next로 교체 (ROTATED)
     * - 저장값 없음 → MISSING (로그아웃·정지 등)
     * - 저장값 != expected → 이미 교체된 RT가 다시 쓰임 = 탈취 의심 → 저장값을 지워 재로그인 유도 (MISMATCH)
     */
    private static final RedisScript<Long> ROTATE_SCRIPT = RedisScript.of("""
            local current = redis.call('GET', KEYS[1])
            if not current then return 0 end
            if current ~= ARGV[1] then
              redis.call('DEL', KEYS[1])
              return -1
            end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """, Long.class);

    public RotationResult rotateRefreshToken(UUID userId, String expected, String next, long ttlMillis) {
        Long result = redisTemplate.execute(ROTATE_SCRIPT, List.of(REFRESH_PREFIX + userId),
                expected, next, String.valueOf(ttlMillis));
        if (result == null || result == 0L) return RotationResult.MISSING;
        return result == 1L ? RotationResult.ROTATED : RotationResult.MISMATCH;
    }

    /**
     * 사용자의 기존 토큰 일괄 무효화: 이 시각(epoch ms) 이전에 발급된 Access Token은 거부된다 (계정 정지 등).
     * TTL은 Access Token 최대 유효 시간 이상이어야 한다 (그 뒤엔 어차피 모두 만료).
     */
    public void revokeTokensIssuedBefore(UUID userId, long epochMillis, long ttlMillis) {
        redisTemplate.opsForValue().set(REVOKED_BEFORE_PREFIX + userId, String.valueOf(epochMillis), Duration.ofMillis(ttlMillis));
    }

    /** 토큰 하나의 폐기 상태 — 로그아웃 블랙리스트와 사용자 일괄 무효화 시각을 한 번의 왕복(MGET)으로 조회 */
    public record Revocation(boolean blacklisted, Long revokedBefore) {}

    public Revocation findRevocation(String accessToken, UUID userId) {
        List<String> values = redisTemplate.opsForValue().multiGet(
                List.of(BLACKLIST_PREFIX + hash(accessToken), REVOKED_BEFORE_PREFIX + userId));
        String blacklisted = values == null ? null : values.get(0);
        String revokedBefore = values == null ? null : values.get(1);
        Long before = null;
        if (revokedBefore != null) {
            try {
                before = Long.parseLong(revokedBefore);
            } catch (NumberFormatException ignored) {
                // 잘못된 값은 무시 (fail-open 대신 블랙리스트·만료 검사는 그대로 적용)
            }
        }
        return new Revocation(blacklisted != null, before);
    }

    public void blacklistAccessToken(String accessToken, long ttlMillis) {
        if (ttlMillis <= 0) return; // 이미 만료된 토큰은 저장할 필요 없음
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + hash(accessToken), "logout", Duration.ofMillis(ttlMillis));
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
