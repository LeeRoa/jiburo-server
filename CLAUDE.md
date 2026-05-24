# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Jiburo 서비스의 백엔드 서버. Java 17 + Spring Boot 3.4.1 + Gradle 기반의 모놀리식 REST API 서버이며, 분실 동물 찾기/제보 기능을 중심으로 OAuth2 소셜 로그인, 실시간 채팅(WebSocket/STOMP), 알림, Cloudflare R2 이미지 업로드를 제공합니다.

## 자주 쓰는 커맨드

### 빌드 / 실행
```bash
./gradlew build                                  # 빌드 (테스트 포함)
./gradlew bootRun                                # 로컬 실행
./gradlew clean build -x test                    # 테스트 스킵 빌드 (Docker 이미지용)
```

### 테스트
```bash
./gradlew test                                   # 전체 테스트 (JUnit 5)
./gradlew test --tests "JiburoServerApplicationTests"   # 단일 테스트 클래스
./gradlew test --tests "*.someMethodName"        # 단일 메서드
```
현재 테스트는 `contextLoads` 스모크 테스트 하나뿐입니다. 새 테스트 추가 시 `src/test/java/com/jiburo/server/` 아래에 도메인 미러링 구조로 추가하세요.

### 로컬 인프라 (MariaDB + Redis)
```bash
docker compose -f compose-dev.yaml up -d         # MariaDB(23306) + Redis(6379) 기동
docker compose -f compose-dev.yaml down          # 종료
# DB 시작 오류 시: down 후 ./dev_mysql_data 삭제 후 재기동
```

### 프로덕션 배포 (Docker)
```bash
./gradlew clean build -x test
docker compose -f compose-prod.yaml up --build -d
```
프로덕션은 환경변수(`DB_PASSWORD`, `JWT_SECRET`, `*_CLIENT_ID/SECRET`, `R2_*`) 주입이 필수입니다.

## 프로젝트 구조

### 전체 트리
```
src/main/
├── java/com/jiburo/server/
│   ├── JiburoServerApplication.java         # @SpringBootApplication, @EnableJpaAuditing, @EnableSpringDataWebSupport(VIA_DTO)
│   ├── domain/                              # 비즈니스 도메인 모듈 (chat / image / notification / post / user)
│   └── global/                              # 횡단 관심사 (config / error / response / log / cache / util / domain / controller / repository / dto)
└── resources/
    ├── application.properties               # spring.profiles.include=oauth,jwt,db,r2
    ├── application-{db,jwt,oauth,r2}.properties   # 프로파일별 분리 설정
    ├── data.sql                             # common_codes 초기 시드 (spring.sql.init.mode=always)
    ├── i18n/{error,validation,common,entity}/messages[_en].properties  # 다국어 번들
    ├── assets/                              # 정적 자산
    └── test-web/                            # WebSocket 수동 테스트 페이지
```

### 도메인 모듈 표준 레이어
각 도메인은 다음의 레이어를 가지며, 큰 도메인은 일부 레이어만 사용합니다.
```
domain/{모듈명}/
├── controller/    # @RestController (HTTP) 또는 @Controller (STOMP). ApiResponse 래핑.
├── service/       # 인터페이스 + Impl 분리 패턴 (XxxService / XxxServiceImpl). 트랜잭션 경계.
├── repository/    # Spring Data + QueryDSL Custom (XxxRepository + XxxRepositoryCustom + XxxRepositoryImpl)
├── dao/           # 일부 도메인에서 repository 대신 사용 (user.UserRepository는 dao에 위치). post.dao는 현재 비어있음.
├── domain/        # JPA 엔티티 (BaseTimeEntity 상속) + enums/ 하위 패키지
├── dto/           # Request/Response/Search/Condition. Java record 선호.
└── config/        # 도메인 전용 빈 (예: chat.config.StompHandler, image.config.R2Config)
```

### 도메인별 책임 한 줄 요약

- **user** — 진입점 `AuthController` (`/api/v1/auth`). 핵심 엔티티 `User` (PK: UUID, JDBC `BINARY`). OAuth2 소셜 로그인 후 JWT 발급/재발급, 회원 프로필, 뱃지/점수 관리. `jwt/` 패키지에 `JwtFilter`, `JwtTokenProvider`, `OAuth2SuccessHandler`, `JwtAuthenticationEntryPoint`.
- **post** — 진입점 `LostPostController` (`/api/v1/posts`). 핵심 엔티티 `LostPost`, `LostPostImage`. 분실/제보 게시글 CRUD + 지도 기반 조회. `detail` 필드는 카테고리에 따라 구조가 바뀌는 JSON 컬럼(`@JdbcTypeCode(SqlTypes.JSON)`). 반경 조회는 QueryDSL + MySQL Haversine 식.
- **chat** — 진입점 두 개: REST `ChatRoomController` (`/api/v1/chat/rooms`)와 STOMP `ChatController` (`@MessageMapping("/chat/rooms/{roomId}")`). 핵심 엔티티 `ChatRoom`, `ChatMessage`, `ChatParticipant`. 게시글 단위 1:N 채팅방, 실시간 전송 + 읽음 처리. `lastChatAt`로 목록 정렬, `idx_chat_room_created` 인덱스로 메시지 페이징 최적화.
- **image** — 진입점 `ImageController` (`/api/v1/images`). 핵심 엔티티 `ImageMeta`. Cloudflare R2 presigned PUT URL 발급 → 클라이언트 직접 업로드 → `/complete` 콜백으로 `ImageStatus` 전환. 전체 누적 10GB 가드.
- **notification** — 진입점 `NotificationController` (`/api/v1/notifications`). 핵심 엔티티 `Notification`. 알림 목록 / 안 읽음 카운트. `typeCode` + `args`(콤마 구분 가변인자) + `targetId` 패턴 — 메시지 본문은 프론트가 다국어 키로 조립.

### 주요 엔티티 관계
```
User (UUID PK)
 ├── 1:N → LostPost (작성자: user_id)
 │         └── 1:N → LostPostImage
 ├── 1:N → LostPost (발견자: finder_id, nullable)
 ├── 1:N → ChatRoom (방장: host_id)
 ├── 1:N → ChatParticipant
 ├── 1:N → ChatMessage (sender_id)
 ├── 1:N → ImageMeta (업로더)
 └── 1:N → Notification (receiver/sender)

LostPost
 ├── N:1 → User (작성자)
 ├── 1:N → ChatRoom (이 게시글로 시작된 채팅방들)
 └── 1:N → LostPostImage
```
- `User.id`는 `UUID`(JDBC BINARY) — 외부 노출은 일반적으로 OAuth provider 식별자 또는 닉네임으로 처리.
- 그 외 엔티티 PK는 모두 `Long IDENTITY` — 외부 노출 시 반드시 `HashidsUtils.encode()`.

### 글로벌 패키지 구성
```
global/
├── config/        # SecurityConfig, WebSocketConfig, QueryDslConfig, RedisConfig,
│                  # MessageSourceConfig, I18nConfig, WebConfig, AsyncConfig, OpenApiConfig, SwaggerConfig
├── error/         # ErrorCode (enum) + JiburoException + GlobalExceptionHandler
├── response/      # ApiResponse<T> — 모든 컨트롤러의 표준 응답 래퍼
├── log/           # @AuditLog 어노테이션 + AuditLogAspect(AOP) + AuditLogEvent/Listener (비동기 DB 저장)
├── cache/         # CommonCodeCache — common_codes 메모리 캐시 (PostConstruct 로드)
├── util/          # HashidsUtils, MessageUtils(i18n 정적 헬퍼), Translatable
├── domain/        # BaseTimeEntity (createdAt/updatedAt), CommonCode 엔티티 + enums/(CommonCodeType, LogActionType)
├── controller/    # CommonCodeController (/api/v1/common-codes — 앱 시작 시 1회 호출용)
├── repository/    # CommonCodeRepository
└── dto/           # CommonCodeResponseDto
```

### 엔드포인트 한눈에

permitAll (인증 불필요):
- `/api/v1/auth/**` → `AuthController` (토큰 재발급)
- `/api/v1/posts/**` → `LostPostController` (조회 중심, 작성/수정도 permitAll이지만 내부에서 `@AuthenticationPrincipal` 사용)
- `/api/v1/common-codes/**` → `CommonCodeController` (앱 시작 시 1회 호출)
- `/ws-jiburo` → STOMP 엔드포인트 (CONNECT 시 `StompHandler`가 JWT 검증)
- `/swagger-ui.html`, `/v3/api-docs/**` → Springdoc Swagger UI
- `/oauth2/authorization/**`, `/login/oauth2/code/**` → Spring Security OAuth2 콜백
- `/h2-console/**`, `/favicon.ico`, `/error`

인증 필요 (`@AuthenticationPrincipal CustomOAuth2User user`):
- `/api/v1/chat/rooms/**` → `ChatRoomController`
- `/api/v1/notifications/**` → `NotificationController`
- `/api/v1/images/**` → `ImageController`

STOMP 채널:
- 발행 (클라 → 서버): `/pub/chat/rooms/{roomId}` (메시지 전송), `/pub/chat/rooms/{roomId}/read` (읽음 처리)
- 구독 (서버 → 클라): `/sub/chat/rooms/{roomId}` (메시지 수신), `/sub/chat/rooms/{roomId}/read` (읽음 영수증)

## 아키텍처

### 패키지 구성
- `com.jiburo.server.domain.{chat, image, notification, post, user}` — 도메인별 모듈. 각 모듈은 `controller / service / repository / domain / dto` 레이어를 유지하며, `post`/`user`는 `dao`도 보유 (Spring Data 리포지토리 인터페이스 위치).
- `com.jiburo.server.global` — 횡단 관심사 (`config`, `error`, `response`, `log`, `cache`, `util`, `domain` 공통 엔티티).

### 응답 / 예외 규약 (반드시 따를 것)
- 컨트롤러는 항상 `ApiResponse<T>`로 래핑해 반환 (`ApiResponse.success(data)` / `ApiResponse.success()`).
- 비즈니스 예외는 `throw new JiburoException(ErrorCode.XXX)`로 던집니다. `ErrorCode`는 `HttpStatus`와 i18n 메시지 키(`error.*`)를 함께 가집니다.
- 새 에러 유형이 필요하면 `global/error/ErrorCode.java`에 항목을 추가하고, `src/main/resources/i18n/error/messages.properties`(+ `_en`)에 메시지 키를 등록.
- 모든 예외는 `GlobalExceptionHandler`가 잡아 자동으로 `ApiResponse.fail()` 형태로 변환합니다 — 컨트롤러에서 try/catch 하지 마세요.

### 외부 노출 ID는 항상 Hashids
DB PK(`Long`)는 외부에 절대 그대로 노출하지 않습니다. `global/util/HashidsUtils`로 `encode/decode`하며, 컨트롤러는 path variable을 `String`으로 받아 `HashidsUtils.decode(id)`로 변환합니다 (잘못된 해시는 `INVALID_IDENTIFIER` 예외). 응답 DTO도 ID 필드는 인코딩된 문자열로 내려보냅니다. salt는 `app.hashids.salt` 프로퍼티.

### 인증 / 인가
- OAuth2 소셜 로그인(Google/Kakao/Naver) + 자체 JWT의 하이브리드. 로그인 성공 시 `OAuth2SuccessHandler`가 JWT를 발급해 프론트 redirect URI로 전달.
- 모든 요청은 `JwtFilter`가 `UsernamePasswordAuthenticationFilter` 앞에서 토큰 검증 → `SecurityContext`에 `CustomOAuth2User`(UUID 기반)를 채웁니다.
- 컨트롤러에서 사용자 정보가 필요하면 `@AuthenticationPrincipal CustomOAuth2User user`로 받아 `user.getUserId()`(UUID) 사용.
- `permitAll` 경로는 `SecurityConfig` 참조 (`/api/v1/posts/**`, `/api/v1/auth/**`, `/ws-jiburo/**`, Swagger, OAuth 콜백 등).

### Common Code 캐시
`common_codes` 테이블은 앱 시작 시 `CommonCodeCache`가 메모리에 로드 (`Map<group, Map<code, CommonCode>>`). 코드 조회는 DB JOIN 없이 `commonCodeCache.get(group, code)`. 신규 enum은 `CommonCodeType` 인터페이스를 구현해야 하며, `data.sql`로 초기 데이터를 시드합니다 (`spring.sql.init.mode=always`).

### 감사 로그 (AOP)
변경 작업이 있는 컨트롤러 메서드에 `@AuditLog(action = LogActionType.XXX)`를 붙이면 `AuditLogAspect`가 `@AfterReturning`으로 가로채 `ApplicationEventPublisher`로 이벤트 발행 → 리스너가 비동기로 DB 저장. **컨트롤러 메서드에서 직접 로그를 남기지 말고 어노테이션을 사용**하세요. 새 액션은 `LogActionType` enum에 추가.

### 영속성 (JPA + QueryDSL)
- 모든 엔티티는 `BaseTimeEntity`(JPA Auditing, `createdAt`/`updatedAt` 자동) 상속.
- `EnableJpaAuditing` + `EnableSpringDataWebSupport(VIA_DTO)` (Page 응답을 DTO로 직렬화).
- 동적 쿼리는 QueryDSL — `JPAQueryFactory` 빈을 주입받아 `repository/*RepositoryImpl`에서 구현. **QClass는 `build/generated/querydsl`에 생성**되므로 엔티티 추가 후엔 빌드(또는 `compileJava`) 필수. `sourceSets`에서 자동 등록되어 있습니다.
- `spring.jpa.open-in-view=false` — 트랜잭션 외부에서 lazy 접근 금지.

### WebSocket / STOMP 채팅
- 엔드포인트: `/ws-jiburo` (SecurityConfig에서 permitAll).
- 구독 prefix `/sub`, 발행 prefix `/pub`.
- `StompHandler`가 인바운드 채널 인터셉터로 STOMP CONNECT 시 JWT 검증.

### 이미지 업로드 (Cloudflare R2)
AWS S3 SDK(`software.amazon.awssdk:s3`)를 R2 엔드포인트에 붙여 사용. 클라이언트는 **presigned PUT URL을 받아 직접 업로드** 후 완료 콜백(`completeUpload`)으로 `ImageStatus`를 `COMPLETED`로 전환합니다. `ImageStorageServiceImpl`이 전체 누적 용량(10GB) 가드를 가짐. 키 구조: `{basePath}/{userId}/{uuid}.{ext}`.

### i18n
- 메시지 번들: `i18n/error/messages*`, `i18n/validation/messages*` (`MessageSourceConfig`에 등록).
- 코드에서는 `MessageUtils.getMessage(key)` 또는 `getMessage(key, args)` 사용. 메시지를 못 찾으면 키 자체를 반환하므로 안전.
- 로케일은 `LocaleContextHolder`(요청 단위)에서 결정.

### 설정 프로파일
`application.properties`가 `spring.profiles.include=oauth, jwt, db, r2`로 다음을 자동 머지:
- `application-db.properties` — MariaDB(로컬 23306) + Redis + JPA
- `application-jwt.properties` — JWT secret/만료
- `application-oauth.properties` — Google/Kakao/Naver client id/secret
- `application-r2.properties` — Cloudflare R2 자격증명/버킷 (모두 `${...}` 환경변수)

## 작업 시 주의사항

- **ID 처리 경로**: 컨트롤러 = 항상 Hashids 문자열 입출력, 서비스/리포지토리 = 항상 `Long`. 경계에서만 변환.
- **상수 enum은 `CommonCodeType` 구현 + `data.sql` 시드 동기화**가 짝. 한쪽만 추가하면 캐시 미스로 런타임 오류.
- **예외 메시지/에러코드 추가 시 i18n 메시지 키 등록을 잊지 말 것** — 누락 시 키 문자열이 그대로 응답됨.
- DEV 환경 DB 비밀번호/Redis 비밀번호는 `compose-dev.yaml`/`application-db.properties`에 하드코딩되어 있습니다. PROD는 환경변수 주입을 사용하세요.
- Swagger UI: 실행 후 `http://localhost:8080/swagger-ui.html`.
