---
name: jiburo-user-auth
description: Use when working on jiburo-server authentication/authorization — OAuth2
  social login (Google/Kakao/Naver), JWT issuance/validation/reissue, JwtFilter,
  StompHandler auth, SecurityConfig permitAll routes, CustomOAuth2User, User entity
  (UUID PK), or anything under domain/user/. Triggers include AuthController,
  JwtTokenProvider, JwtFilter, OAuth2SuccessHandler, JwtAuthenticationEntryPoint,
  CustomOAuth2User, CustomOAuth2UserService, SecurityConfig, /oauth2/authorization,
  /login/oauth2/code, /api/v1/auth/reissue, /ws-jiburo, 로그인, 토큰, 인증, 인가, JWT,
  OAuth2, 소셜 로그인.
---

# Jiburo User / Auth 도메인 가이드

OAuth2 소셜 로그인 + 자체 JWT 하이브리드. **세션 STATELESS** + 매 요청 JWT 검증.

## 진입점

| 종류 | 클래스 | 경로 |
|---|---|---|
| REST | `AuthController` | `/api/v1/auth` — 현재 `POST /reissue`만 |
| OAuth2 시작 | Spring Security | `/oauth2/authorization/{google,kakao,naver}` |
| OAuth2 콜백 | Spring Security | `/login/oauth2/code/{provider}` |

추가 사용자 정보 API는 아직 없음 (TODO).

## 핵심 파일

- `global/config/SecurityConfig.java` — 전체 보안 체인, `permitAll` 화이트리스트
- `domain/user/jwt/JwtFilter.java` — 매 요청 토큰 검증 → `SecurityContext` 채움
- `domain/user/jwt/JwtTokenProvider.java` — 토큰 생성/검증/Authentication 변환
- `domain/user/jwt/OAuth2SuccessHandler.java` — 로그인 성공 후 신규 가입 + JWT 발급 + redirect
- `domain/user/jwt/JwtAuthenticationEntryPoint.java` — 인증 실패 시 401 응답
- `domain/user/service/CustomOAuth2UserService.java` — OAuth provider 응답 파싱
- `domain/user/dto/CustomOAuth2User.java` — `SecurityContext` principal (UUID 기반)
- `domain/user/domain/User.java` — UUID PK 엔티티
- `domain/user/dao/UserRepository.java` (참고: `repository`가 아닌 `dao` 위치)
- `domain/chat/config/StompHandler.java` — STOMP CONNECT 시 JWT 검증

## 인증 플로우

### (1) 최초 소셜 로그인
```
프론트 → /oauth2/authorization/google
  → Spring Security OAuth2가 provider로 redirect
  → 사용자 동의 후 /login/oauth2/code/google 콜백
  → CustomOAuth2UserService.loadUser() — provider 응답 파싱
  → OAuth2SuccessHandler.onAuthenticationSuccess()
      ├─ oauthId = "google_<providerId>"
      ├─ User 없으면 joinNewUser() (BadgeType.BEGINNER, activityScore=0, RoleType.USER)
      ├─ CustomOAuth2User(userId=UUID) 생성
      ├─ JwtTokenProvider.generateTokenDto(authentication) — sub=UUID string
      ├─ AuditLogEvent.AUTH_LOGIN 발행
      └─ ${app.oauth2.authorized-redirect-uri}?accessToken=...&refreshToken=... 로 redirect
```

### (2) 일반 API 요청
```
요청 → JwtFilter.doFilterInternal
  ├─ Authorization 헤더에서 "Bearer <jwt>" 추출
  ├─ validateToken(jwt) — 서명/만료 검증 (실패해도 필터 체인 계속, permitAll 경로 위해)
  └─ getAuthentication(jwt)
      ├─ claims.sub = UUID string → UUID.fromString
      ├─ CustomOAuth2User(userId, authorities) 생성
      └─ SecurityContext에 저장
컨트롤러 → @AuthenticationPrincipal CustomOAuth2User user → user.getUserId() : UUID
```

### (3) STOMP 연결
```
WebSocket CONNECT → StompHandler.preSend
  ├─ STOMP 헤더 "Authorization: Bearer ..." 읽음
  ├─ validateToken 실패 시 throw JiburoException(INVALID_TOKEN)
  └─ accessor.setUser(authentication) — 세션에 인증 객체 저장
이후 @MessageMapping → Authentication authentication.getPrincipal() = CustomOAuth2User
```

### (4) 토큰 재발급
`POST /api/v1/auth/reissue` (`TokenRequestDto`) → `AuthService.reissue` → `TokenResponseDto`

## 핵심 설정 (SecurityConfig)

- **세션 STATELESS** — 서버 세션 안 만듦
- **CSRF/formLogin/httpBasic disabled**
- **CORS** — `setAllowedOriginPatterns("*")` (운영 환경에서는 제한 필요)
- **`addFilterBefore(JwtFilter, UsernamePasswordAuthenticationFilter.class)`**

### permitAll 화이트리스트
```
/ws-jiburo/**            ← STOMP는 핸들러에서 별도 검증
/api/v1/posts/**         ← 컨트롤러 내부에서 @AuthenticationPrincipal로 식별
/api/v1/common-codes/**
/api/v1/auth/**
/oauth2/authorization/**, /login/oauth2/code/**
/v3/api-docs/**, /swagger-ui/**, /swagger-ui.html
/h2-console/**, /favicon.ico, /error
```
그 외는 모두 `authenticated()`.

## 엔티티 / DB

```
User (PK UUID, JDBC BINARY)
 ├── oauth_id (unique)        ← "google_xxx" / "kakao_xxx" / "naver_xxx"
 ├── nickname (not null)
 ├── email, profile_image_url
 ├── role_code (ENUM RoleType: USER, ADMIN) — Spring Security "ROLE_USER"
 ├── badge_code (ENUM BadgeType: BEGINNER, SENIOR, ...)  ← CommonCodeType
 └── activity_score (int)
```

`User.id`는 **UUID(BINARY)** — 다른 엔티티들과 달리 Hashids 대상이 **아님**.

## 도메인 규약

1. **사용자 식별자는 무조건 `CustomOAuth2User.getUserId()` (UUID)**. 외부 노출이 필요하면 `oauth_id`/`nickname` 사용 (UUID는 노출 금지).
2. **JWT `sub`는 UUID 문자열** — `claims.getSubject()` → `UUID.fromString`. 기존 토큰 형식을 바꾸면 발급된 모든 토큰이 깨짐.
3. **`Authorities`는 `"ROLE_USER"` 형태** — `User.getRoleKey()`가 `"ROLE_" + roleCode` 반환. `@PreAuthorize("hasRole('ADMIN')")` 등 사용 가능 (`@EnableMethodSecurity` 켜져 있음).
4. **`JwtFilter`는 토큰 없거나 무효해도 throw하지 않음** — 그래야 `permitAll` 경로가 로그인 안 한 사용자도 접근 가능. 인증 실패 401은 `JwtAuthenticationEntryPoint`가 처리.
5. **STOMP는 다른 흐름** — `StompHandler`는 토큰이 있는데 무효하면 즉시 `JiburoException(INVALID_TOKEN)` throw (REST와 다름).
6. **신규 가입 자동 처리** — `OAuth2SuccessHandler`가 `findByOauthId` 실패 시 자동으로 `joinNewUser`. 닉네임 부재 시 `"User_" + UUID 8자리`.
7. **로그인 자체는 AOP가 잡을 수 없음** — `OAuth2SuccessHandler`에서 직접 `eventPublisher.publishEvent(AUTH_LOGIN)` 발행 (예외적 케이스).

## OAuth Provider 추가

1. `application-oauth.properties`에 `spring.security.oauth2.client.registration.{provider}.{client-id,client-secret,scope,redirect-uri}` 추가
2. `CustomOAuth2UserService` / `OAuthAttributes`에서 provider별 응답 파싱 분기
3. `oauthId` 포맷은 `"{provider}_{providerId}"` 규칙 유지

## 자주 하는 실수

- ❌ `User.id`를 Hashids로 인코딩 → ✅ UUID 그대로 사용하되 **외부 노출은 nickname/oauthId**로
- ❌ `@AuthenticationPrincipal` 없이 `SecurityContextHolder` 직접 호출 → 가능은 하지만 컨트롤러에서는 어노테이션 방식 사용
- ❌ `permitAll` 경로에서 `user.getUserId()` 호출하기 전 null 체크 안 함 → 로그인 안 된 사용자 NPE
- ❌ JWT secret을 평문 application.properties에 → ✅ `${JWT_SECRET}` 환경변수
- ❌ `JwtTokenProvider.getAuthentication`에서 `sub` UUID 파싱 실패 시 처리 부재 → 잘못된 토큰 들어오면 `IllegalArgumentException`
- ❌ STOMP CONNECT 시 헤더 키를 `"jwt"` 같이 사용자 정의 → ✅ **`Authorization: Bearer ...`** 유지 (`StompHandler`가 그렇게 읽음)
- ❌ refresh token을 DB에 저장 안 하는데 무한 재발급 허용 → 현재 `JwtTokenProvider`는 refresh 검증 정책이 단순. 정책 변경 시 reissue 흐름 재검토 필요.

## ErrorCode

- `UNAUTHORIZED` (401)
- `ACCESS_DENIED` (403) — 관리자 페이지 등
- `INVALID_TOKEN` (401) — STOMP에서 사용
- `EXPIRED_TOKEN` (401)
- `INVALID_REFRESH_TOKEN` (401)
- `OAUTH_PROVIDER_FAILED` / `OAUTH_PROVIDER_INVALID` (502)
- `USER_NOT_FOUND` (404)
- `DUPLICATE_EMAIL` / `DUPLICATE_NICKNAME` (409)

## 관련 skill

- [[jiburo-conventions]] — `ApiResponse`/`JiburoException`/`@AuditLog` 등
- [[jiburo-chat]] — `StompHandler`가 이 도메인의 `JwtTokenProvider`를 직접 사용
