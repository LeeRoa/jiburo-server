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
│   ├── domain/                              # 비즈니스 도메인 모듈 (admin / chat / emoticon / image / moderation / notification / post / report / user 등)
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

### 도메인별 스킬 가이드

각 도메인 작업 시 아래의 스킬을 `/스킬명` 형식으로 먼저 호출하면 해당 도메인의 상세 가이드를 참조할 수 있습니다.

| 도메인 | 스킬 | 주요 트리거 키워드 |
|--------|------|-------------------|
| **user / 인증·인가** | `/jiburo-user-auth` | `AuthController`, `JwtTokenProvider`, `JwtFilter`, `OAuth2SuccessHandler`, `SecurityConfig`, `CustomOAuth2User`, 로그인, 토큰, JWT, OAuth2, 소셜 로그인 |
| **post / 게시글** | `/jiburo-post` | `LostPostController`, `LostPost`, `LostPostImage`, `LostPostDetailConverter`, Haversine, viewport, radius, `/api/v1/posts`, 분실, 제보, 게시글, 지도, 반경 |
| **chat / 채팅** | `/jiburo-chat` | `ChatRoomController`, `ChatController`, `StompHandler`, `ChatRoom`, `ChatMessage`, `ChatParticipant`, `/ws-jiburo`, `/pub/chat`, `/sub/chat`, 채팅방, STOMP, WebSocket, 읽음 처리 |
| **image / 이미지** | `/jiburo-image` | `ImageController`, `ImageStorageService`, `ImageMeta`, `ImageStatus`, `R2Config`, presigned, R2, 이미지 업로드, 파일 업로드 |
| **notification / 알림** | `/jiburo-notification` | `NotificationController`, `Notification`, `/api/v1/notifications`, 알림, typeCode, 읽음 카운트 |
| **global / 공통 규약** | `/jiburo-conventions` | `ApiResponse`, `JiburoException`, `ErrorCode`, `HashidsUtils`, `@AuditLog`, `CommonCodeCache`, `CommonCodeType`, `MessageUtils`, `GlobalExceptionHandler`, `BaseTimeEntity`, 새 에러 코드, 공통 코드, ID 노출, 응답 래핑, 감사 로그 |
| **테스트 코드** | `/jiburo-testing` | `@Test`, `@WebMvcTest`, `@SpringBootTest`, `@DataJpaTest`, `MockMvc`, `Mockito`, `@MockBean`, `@WithMockUser`, `assertThat`, `JiburoException`, 테스트, 단위 테스트, 통합 테스트, 컨트롤러 테스트, 슬라이스 테스트 |
| **커밋 & 브랜치** | `/jiburo-commit` | git commit, git branch, 커밋, 브랜치, PR, 머지, squash, 커밋 메시지, 브랜치 이름, 작업 시작, 작업 완료, push |

> 스킬은 해당 도메인에 해당하는 작업을 시작하기 전에 호출하세요. 여러 도메인에 걸친 작업이라면 관련된 스킬을 모두 참조하세요.

### 도메인별 책임 한 줄 요약

- **user** — 진입점 `AuthController` (`/api/v1/auth`). 핵심 엔티티 `User` (PK: UUID, JDBC `BINARY`). OAuth2 소셜 로그인 후 JWT 발급/재발급, 회원 프로필, 뱃지/점수 관리. `jwt/` 패키지에 `JwtFilter`, `JwtTokenProvider`, `OAuth2SuccessHandler`, `JwtAuthenticationEntryPoint`.
- **post** — 진입점 `LostPostController` (`/api/v1/posts`). 핵심 엔티티 `LostPost`, `LostPostImage`. 분실/제보 게시글 CRUD + 지도 기반 조회. `detail` 필드는 카테고리에 따라 구조가 바뀌는 JSON 컬럼(`@JdbcTypeCode(SqlTypes.JSON)`). 반경 조회는 QueryDSL + MySQL Haversine 식.
- **chat** — 진입점 두 개: REST `ChatRoomController` (`/api/v1/chat/rooms`)와 STOMP `ChatController` (`@MessageMapping("/chat/rooms/{roomId}")`). 핵심 엔티티 `ChatRoom`, `ChatMessage`, `ChatParticipant`. 게시글 단위 1:N 채팅방, 실시간 전송 + 읽음 처리. `lastChatAt`로 목록 정렬, `idx_chat_room_created` 인덱스로 메시지 페이징 최적화.
- **image** — 진입점 `ImageController` (`/api/v1/images`). 핵심 엔티티 `ImageMeta`. Cloudflare R2 presigned PUT URL 발급 → 클라이언트 직접 업로드 → `/complete` 콜백으로 `ImageStatus` 전환. 전체 누적 10GB 가드.
- **emoticon** — 진입점 `EmoticonController` (`GET /api/v1/chat/emoticons`). 핵심 엔티티 `EmoticonPack`, `Emoticon`. 관리자가 등록한 무료 이모티콘(png / 움직이는 gif·webp). 노출 묶음 목록은 메모리 캐시(`EmoticonService.invalidateCatalog`, 60초 주기). 채팅 `EMOTICON` 메시지는 이모티콘 ID로 받아 **이미지 키로 저장** → 이모티콘을 지워도 과거 메시지 유지. 관리 API는 `AdminEmoticonController`.
- **moderation** — 금지어. 핵심 엔티티 `BannedWord`, 핵심 컴포넌트 `BannedWordFilter`(`mask` / `contains`). 채팅 텍스트(TALK)·게시글 제목/내용은 **`***`로 가린 값을 저장**(원문 미저장). 대소문자 무시, 글자 사이 기호까지 매칭, 공백은 오탐 때문에 미허용. 메모리 캐시(`invalidate`, 60초 주기). 관리 API는 `AdminBannedWordController`. 닉네임은 가리지 않고 거부(`NICKNAME_CONTAINS_BANNED_WORD`) — 닉네임 변경 API(mypage 브랜치) 머지 후 연결 필요.
- **report** — 진입점 `ReportController` (`POST /api/v1/reports`). 핵심 엔티티 `Report`. 게시글·채팅 메시지·사용자 신고, `target_key`로 같은 대상 묶음(중복 신고 차단·일괄 처리), 신고 당시 내용 스냅샷. 처리는 `AdminReportController`(콘텐츠 삭제 / 정지 / 기각 + 신고자 알림 `REPORT_HANDLED`).
- **계정 정지** — `User.suspendedUntil`(9999-12-31 = 영구). `UserSuspensionService`가 정지 + Refresh Token 삭제, `AuthTokenService.issueTokens`가 정지 계정의 로그인·재발급을 `USER_SUSPENDED`로 막음 (웹 OAuth는 `?error=USER_SUSPENDED` 리다이렉트). 이미 받은 Access Token은 만료까지 유효. 관리자 계정은 정지 불가.
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
- `/api/v1/common-codes/**` → `CommonCodeController` (앱 시작 시 1회 호출, 각 코드에 요청 언어 `label` 포함)
- `/ws-jiburo` → STOMP 엔드포인트 (CONNECT 시 `StompHandler`가 JWT 검증)
- `/swagger-ui.html`, `/v3/api-docs/**` → Springdoc Swagger UI
- `/oauth2/authorization/**`, `/login/oauth2/code/**` → Spring Security OAuth2 콜백
- `/favicon.ico`, `/error`

인증 필요 (`@AuthenticationPrincipal CustomOAuth2User user`):
- `/api/v1/chat/rooms/**` → `ChatRoomController`
- `/api/v1/chat/emoticons` → `EmoticonController` (채팅 이모티콘 목록)
- `/api/v1/reports` → `ReportController` (신고 접수)
- `/api/v1/notifications/**` → `NotificationController`
- `/api/v1/images/**` → `ImageController`

관리자 전용 (`ROLE_ADMIN`, 경로 단위 보호):
- `/api/v1/admin/**` → `domain/admin/controller/Admin*Controller` (아래 "관리자 API" 참고)

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
- `audit_logs.user_id`는 NOT NULL이라 비로그인 요청은 AOP가 기록을 건너뜁니다. 로그인/토큰 재발급처럼 인증 전 시점의 작업은 서비스에서 userId를 채워 `AuditLogEvent`를 직접 발행합니다.
- `targetData`에는 로그인 사용자/요청 객체를 제외한 메서드 인자(경로 변수, 요청 DTO)가 기록됩니다. 민감 정보가 있는 DTO는 `toString()`을 마스킹하세요 (예: `TokenRequestDto`).

### 관리자 API (`domain/admin`)
- 모든 관리자 컨트롤러는 `AdminApi.BASE_PATH`(`/api/v1/admin`) 아래에 매핑합니다. `SecurityConfig`가 이 경로를 `hasRole("ADMIN")`으로 1차 보호합니다 (ADMIN = 관리자 역할 1개 이상 보유 계정).
- **권한 모델**: 메뉴(`AdminMenu` enum, 코드 고정) × 접근 수준(READ=노출·조회, WRITE=편집·변경). 역할(`admin_roles`)은 메뉴 권한의 묶음으로 관리자 웹에서 관리하고, 계정에 여러 개 부여합니다(`user_admin_roles`). `SUPER_ADMIN`은 시스템 역할로 모든 메뉴 WRITE.
- **엔드포인트마다 메뉴 권한을 반드시 선언**: `@PreAuthorize("@adminAccess.read('SETTING')")`(GET) / `@PreAuthorize("@adminAccess.write('SETTING')")`(변경) / `@PreAuthorize("@adminAccess.any()")`(역할만 있으면). 누락·오타·변경 API에 read 사용은 `AdminControllerSecurityConventionTest`가 실패시킵니다.
- 권한은 JWT가 아니라 `AdminAuthorizationService`(메모리 캐시, 60초 주기 비우기)로 매 요청 확인 → 회수가 즉시 반영됩니다. 역할/메뉴 권한을 바꾸는 코드는 반드시 `invalidate`를 호출하세요.
- 새 관리자 기능 = `AdminMenu`에 메뉴 추가 + API에 `@PreAuthorize`. 새 메뉴는 SUPER_ADMIN 외 어떤 역할에도 자동으로 열리지 않습니다.
- 초기 관리자: 환경변수 `ADMIN_OAUTH_IDS`(예: `kakao_123`)에 적힌 계정은 로그인 시 SUPER_ADMIN을 자동으로 받습니다 (`AdminBootstrapService`).
- 권한 부족은 403 `ApiResponse`(`ACCESS_DENIED`)로 응답합니다 (경로 단계: `JwtAccessDeniedHandler`, 메서드 단계: `GlobalExceptionHandler`).
- `@EnableMethodSecurity(proxyTargetClass = true)`는 유지하세요. 컨트롤러가 Docs 인터페이스를 구현하므로 인터페이스 프록시가 되면 매핑이 사라져 404가 납니다.
- 관리자 컨트롤러는 `domain/admin/controller`, 관리자 전용 로직은 `domain/admin/service`에 두고, 각 도메인의 서비스/리포지토리를 재사용합니다.
- 상태를 바꾸는 관리자 API에는 `@AuditLog(action = LogActionType.ADMIN_XXX)`를 반드시 붙입니다.
- 현재 메뉴: 대시보드, 운영 설정, 공통코드, 다국어 문구(`AdminI18nController`), 사용자(정지·해제 포함), 게시글, 신고(`AdminReportController`), 이모티콘(`AdminEmoticonController`), 금지어(`AdminBannedWordController`), 감사 로그, 시스템 모니터링(`AdminMonitoringController`), 역할·권한.
- 시스템 모니터링은 요청을 받은 서버 기준 값(DB·Redis 응답, JVM, 저장소, 최근 500 오류). 최근 오류는 `ServerErrorRecorder`가 `GlobalExceptionHandler`의 500을 메모리에 50건 보관 — 재시작 시 비워짐.
- 이모티콘 업로드: `POST /api/v1/admin/emoticons/presigned-url` → R2 PUT → `/api/v1/images/complete` → `POST /packs/{packId}/emoticons`. `UploadTargetType.EMOTICON`은 `isAdminOnly()`라 일반 업로드 API에서 거부됩니다.
- 테스트는 `AdminControllerTestSupport`를 상속하면 보안 설정과 `withAdmin()`/`withUser()` 헬퍼를 그대로 쓸 수 있습니다.

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
- 문구 출처: `classpath*:i18n/**/messages*.properties`(파일 기본값, 접미사 없는 파일 = ko) + `i18n_messages` 테이블(관리자 수정값). `MessageCatalog`가 둘을 합치고 `CatalogMessageSource`가 MessageSource로 노출합니다.
- 조회 순서(요청 언어 L): DB[L] → 파일[L] → DB[ko] → 파일[ko]. `zh-CN` → `zh` 순 대체.
- 관리자 수정은 `AdminI18nService` → 저장 후 `I18nMessageCache.reload()` (다른 서버는 60초 주기). 모든 문구가 MessageFormat으로 처리되므로 작은따옴표는 `''`로 씁니다.
- 코드에서는 `MessageUtils.getMessage(key)` 또는 `getMessage(key, args)` 사용. 메시지를 못 찾으면 키 자체를 반환하므로 안전.
- 화면 라벨처럼 "없으면 비워야 하는" 값은 `LabelResolver.resolve(key)` (없으면 null).
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
