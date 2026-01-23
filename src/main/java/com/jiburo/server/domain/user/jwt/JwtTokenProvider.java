package com.jiburo.server.domain.user.jwt;

import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.error.BusinessException;
import com.jiburo.server.global.error.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";

    private final Key key;
    private final long accessTokenValidityTime;
    private final long refreshTokenValidityTime;

    // application.properties에서 값을 읽어와서 초기화
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

    // 1. 토큰 생성 (로그인 성공 시 호출)
    public TokenResponseDto generateTokenDto(Authentication authentication) {
        // 권한들 가져오기 (ROLE_USER 등)
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성
        Date accessTokenExpiresIn = new Date(now + accessTokenValidityTime);
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())       // payload "sub": "kakao_12345"
                .claim(AUTHORITIES_KEY, authorities)        // payload "auth": "ROLE_USER"
                .setExpiration(accessTokenExpiresIn)        // payload "exp": 1516239022 (예시)
                .signWith(key, SignatureAlgorithm.HS256)    // header "alg": "HS256"
                .compact();

        // Refresh Token 생성 (만료일자만 설정)
        String refreshToken = Jwts.builder()
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

    // 2. 토큰에서 인증 정보(Authentication) 꺼내기
    // 필터에서 Access Token이 유효한지 검증 후, SecurityContext에 저장할 객체를 만듦
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE); // 권한 정보가 없는 토큰 예외 처리
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴
        // (여기서는 비밀번호가 필요 없으므로 빈 문자열 처리)
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 3. 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // 토큰 파싱 헬퍼 메서드
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims(); // 만료된 토큰이어도 클레임은 꺼내서 보여줌
        }
    }
}