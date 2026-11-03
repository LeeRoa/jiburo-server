package com.jiburo.server.domain.user.jwt;

import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String TOKEN_TYPE_KEY = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String BEARER_TYPE = "Bearer";

    private final Key key;
    private final long accessTokenValidityTime;
    private final long refreshTokenValidityTime;

    // application.properties에서 값을 읽어와서 초기화 (프로퍼티 이름과 달리 단위는 ms)
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityTime,
            @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityTime
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityTime = accessTokenValidityTime;
        this.refreshTokenValidityTime = refreshTokenValidityTime;
    }

    /**
     * Access/Refresh 토큰 쌍 생성.
     * - 두 토큰 모두 sub = userId(UUID 문자열), typ 클레임으로 용도를 구분한다.
     * - Refresh Token에는 jti(랜덤 UUID)를 넣어 같은 시각에 발급돼도 값이 달라지도록 한다 (Rotation 비교용).
     */
    public TokenResponseDto generateTokenDto(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date accessTokenExpiresIn = new Date(now + accessTokenValidityTime);

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim(TOKEN_TYPE_KEY, TOKEN_TYPE_ACCESS)
                .setIssuedAt(new Date(now))
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(TOKEN_TYPE_KEY, TOKEN_TYPE_REFRESH)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshTokenValidityTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenResponseDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .build();
    }

    /**
     * 유효한 Access Token에서 인증 정보(Authentication)를 꺼낸다.
     * Refresh Token이나 typ 클레임이 없는 구버전 토큰은 거부한다.
     */
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseValidClaims(accessToken)
                .filter(c -> TOKEN_TYPE_ACCESS.equals(c.get(TOKEN_TYPE_KEY)))
                .filter(c -> c.get(AUTHORITIES_KEY) != null)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_TOKEN));

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UUID userId = parseUserId(claims);
        CustomOAuth2User principal = new CustomOAuth2User(userId, authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 서명/만료 검증
    public boolean validateToken(String token) {
        return parseValidClaims(token).isPresent();
    }

    // 서명/만료 검증 + Access Token 용도 확인
    public boolean validateAccessToken(String token) {
        return parseValidClaims(token)
                .map(c -> TOKEN_TYPE_ACCESS.equals(c.get(TOKEN_TYPE_KEY)))
                .orElse(false);
    }

    // 서명/만료 검증 + Refresh Token 용도 확인
    public boolean validateRefreshToken(String token) {
        return parseValidClaims(token)
                .map(c -> TOKEN_TYPE_REFRESH.equals(c.get(TOKEN_TYPE_KEY)))
                .orElse(false);
    }

    // 유효한 토큰의 sub(userId) 추출
    public UUID getUserId(String token) {
        Claims claims = parseValidClaims(token)
                .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_TOKEN));
        return parseUserId(claims);
    }

    // 남은 유효시간(ms). 이미 만료/무효면 0
    public long getRemainingValidityMillis(String token) {
        return parseValidClaims(token)
                .map(c -> Math.max(0L, c.getExpiration().getTime() - System.currentTimeMillis()))
                .orElse(0L);
    }

    public long getRefreshTokenValidityTime() {
        return refreshTokenValidityTime;
    }

    private UUID parseUserId(Claims claims) {
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JiburoException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Optional<Claims> parseValidClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody());
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return Optional.empty();
    }
}
