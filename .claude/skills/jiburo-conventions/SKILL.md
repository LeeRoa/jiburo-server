---
name: jiburo-conventions
description: Use when working on jiburo-server cross-cutting conventions — adding/modifying
  controllers/endpoints, throwing business errors, returning API responses, encoding/decoding
  external IDs (Hashids), adding audit logs, registering common codes (enum + data.sql),
  or wiring i18n messages. Triggers include keywords like ApiResponse, JiburoException,
  ErrorCode, HashidsUtils, @AuditLog, CommonCodeCache, CommonCodeType, MessageUtils,
  GlobalExceptionHandler, BaseTimeEntity, "새 에러 코드", "공통 코드 추가", "ID 노출",
  "응답 래핑", "감사 로그".
---

# Jiburo 횡단 규약 (Cross-cutting Conventions)

jiburo-server의 모든 도메인이 공유하는 공통 규약. 새 컨트롤러/서비스/엔티티/에러를 추가하거나, 외부에 ID를 노출하거나, 변경 작업을 기록할 때 반드시 따를 것.

## 1. API 응답 — 항상 `ApiResponse<T>`로 래핑

**파일:** `src/main/java/com/jiburo/server/global/response/ApiResponse.java`

컨트롤러는 절대로 raw DTO/엔티티/`ResponseEntity`를 반환하지 않는다. 항상 `ApiResponse<T>`로 래핑.

```java
// 데이터 있는 성공
return ApiResponse.success(responseDto);

// 데이터 없는 성공 (생성/삭제 등)
return ApiResponse.success();
```

`fail(...)`은 **컨트롤러에서 직접 호출하지 않는다**. `GlobalExceptionHandler`가 예외를 잡아 자동 변환한다.

응답 JSON 구조:
```json
{ "success": true,  "status": 200, "errorCode": null, "message": null, "data": {...} }
{ "success": false, "status": 404, "errorCode": "POST_NOT_FOUND", "message": "...", "data": null }
```

## 2. 예외 — `JiburoException(ErrorCode.XXX)`만 던진다

**파일:**
- `src/main/java/com/jiburo/server/global/error/JiburoException.java`
- `src/main/java/com/jiburo/server/global/error/ErrorCode.java`
- `src/main/java/com/jiburo/server/global/error/GlobalExceptionHandler.java`

비즈니스 예외는 **항상** `JiburoException`으로 던진다. 컨트롤러/서비스에서 `try/catch`로 응답을 만들지 않는다.

```java
throw new JiburoException(ErrorCode.POST_NOT_FOUND);
```

### 새 에러를 추가하는 4단계 (하나라도 빠뜨리면 안 됨)

1. `ErrorCode` enum에 항목 추가 — `HttpStatus` + 메시지 키
   ```java
   POST_NOT_FOUND(HttpStatus.NOT_FOUND, "error.post.not_found"),
   ```
2. `src/main/resources/i18n/error/messages.properties`에 한글 메시지 등록
   ```properties
   error.post.not_found=게시글을 찾을 수 없습니다.
   ```
3. `src/main/resources/i18n/error/messages_en.properties`에 영문 메시지 등록
4. 코드에서 `throw new JiburoException(ErrorCode.POST_NOT_FOUND)`

**i18n 키를 등록하지 않으면 응답 message에 키 문자열 그대로 노출된다** (`MessageUtils`가 폴백으로 키 반환).

### ErrorCode 카테고리 (참고)
- `[1] Global` — `INTERNAL_SERVER_ERROR`, `INVALID_INPUT_VALUE`, `INVALID_IDENTIFIER` 등
- `[2] Auth & User` — `UNAUTHORIZED`, `USER_NOT_FOUND`, `INVALID_TOKEN` 등
- `[3] Animal & Map` — `ANIMAL_NOT_FOUND` 등
- `[4] Image` — `FILE_SIZE_EXCEEDED`, `INVALID_FILE_FORMAT` 등
- `[5] Chat` — `CHAT_ROOM_NOT_FOUND`, `NOT_CHAT_PARTICIPANT` 등
- `[6] Post` — `POST_NOT_FOUND`, `POST_ACCESS_DENIED` 등

새 카테고리는 enum 안에 주석 헤더와 함께 추가.

## 3. 외부 ID는 **반드시** Hashids 인코딩

**파일:** `src/main/java/com/jiburo/server/global/util/HashidsUtils.java`

DB PK(`Long`)를 외부에 직접 노출하지 않는다. 컨트롤러 경계에서만 변환:

```java
// 컨트롤러: String으로 받아 decode
@GetMapping("/{id}")
public ApiResponse<PostResponse> get(@PathVariable String id) {
    Long postId = HashidsUtils.decode(id);   // 잘못된 해시면 INVALID_IDENTIFIER 예외
    return ApiResponse.success(postService.get(postId));
}

// 응답 DTO: ID 필드는 String (encode된 값)
public record PostResponse(String id, String title, ...) {
    public static PostResponse from(Post entity) {
        return new PostResponse(HashidsUtils.encode(entity.getId()), entity.getTitle(), ...);
    }
}
```

**예외: `User.id`는 UUID(`BINARY`)** — Hashids 대상이 아님. 외부 노출 시 OAuth provider 식별자나 닉네임 사용.

salt는 `app.hashids.salt` 프로퍼티. `HashidsUtils.decode` 실패 시 `JiburoException(INVALID_IDENTIFIER)` 자동 발생.

## 4. 변경 작업은 `@AuditLog`로 기록 (직접 로그 호출 금지)

**파일:**
- `src/main/java/com/jiburo/server/global/log/annotation/AuditLog.java`
- `src/main/java/com/jiburo/server/global/log/AuditLogAspect.java`
- `src/main/java/com/jiburo/server/global/domain/enums/LogActionType.java`

CREATE/UPDATE/DELETE류 컨트롤러 메서드에 어노테이션만 붙인다. AOP가 `@AfterReturning`으로 가로채 이벤트 발행 → 리스너가 비동기 DB 저장.

```java
@PostMapping
@AuditLog(action = LogActionType.POST_CREATE)
public ApiResponse<PostResponse> create(@RequestBody PostCreateRequest request) {
    return ApiResponse.success(postService.create(request));
}
```

주의:
- **컨트롤러 메서드에서 `log.info(...)`로 변경 로그를 직접 남기지 않는다.**
- 새 액션은 `LogActionType` enum에 추가.
- 메서드가 예외를 던지면 로그가 남지 않는다 (`@AfterReturning`이라서). 성공한 작업만 기록됨.
- 첫 번째 파라미터의 `toString()`이 `targetData`로 기록되므로, 민감 정보가 포함된 DTO면 `toString()` 오버라이드 검토.

## 5. 공통 코드 — `CommonCodeType` 구현 + `data.sql` 시드 (짝)

**파일:**
- `src/main/java/com/jiburo/server/global/domain/enums/CommonCodeType.java`
- `src/main/java/com/jiburo/server/global/cache/CommonCodeCache.java`
- `src/main/resources/data.sql`

상수성 enum(역할, 상태, 뱃지, 카테고리 등)은 DB `common_codes` 테이블과 enum을 **양쪽 모두** 유지한다.

### 새 enum 추가 절차
1. enum이 `CommonCodeType` 인터페이스 구현
   ```java
   public enum BadgeType implements CommonCodeType {
       BEGINNER("BADGE", "BEGINNER", "badge.beginner"),
       ...;
       private final String groupCode, code, descriptionKey;
       @Override public String getGroupCode() { return groupCode; }
       @Override public String getCode() { return code; }
       @Override public String getDescriptionKey() { return descriptionKey; }
   }
   ```
2. `data.sql`에 동일한 row를 INSERT (group_code, code 일치 필수)
3. (선택) `i18n/common/messages*.properties`에 `descriptionKey`에 해당하는 다국어 등록

### 캐시 조회
```java
@RequiredArgsConstructor
class SomeService {
    private final CommonCodeCache commonCodeCache;

    void use() {
        commonCodeCache.get("BADGE", "BEGINNER")   // Optional<CommonCode>
                       .orElseThrow(() -> new JiburoException(ErrorCode.BADGE_NOT_FOUND));

        commonCodeCache.getGroup("BADGE");         // List<CommonCode> — 드롭다운용
    }
}
```

`spring.sql.init.mode=always`라 매 기동 시 `data.sql` 실행. enum과 `data.sql` 어느 한쪽만 추가하면 캐시 미스로 런타임 오류.

## 6. i18n — `MessageUtils.getMessage(key, args?)`

**파일:** `src/main/java/com/jiburo/server/global/util/MessageUtils.java`

번들 경로: `src/main/resources/i18n/{error,validation,common,entity}/messages[_en].properties`
로케일: `LocaleContextHolder` (요청 단위 자동)
폴백: 키를 못 찾으면 **키 문자열 그대로 반환** (NPE 안 남)

```java
String msg = MessageUtils.getMessage("error.post.not_found");
String msg2 = MessageUtils.getMessage("greeting.hello", new Object[]{ "지영" });
```

## 7. 엔티티 — `BaseTimeEntity` 상속

**파일:** `src/main/java/com/jiburo/server/global/domain/BaseTimeEntity.java`

새 엔티티는 거의 항상 `BaseTimeEntity`를 상속하여 `createdAt`/`updatedAt`을 자동 채움 (JPA Auditing). `@EnableJpaAuditing`은 `JiburoServerApplication`에 이미 적용됨.

PK 정책:
- `User` → `UUID` (JDBC `BINARY`)
- 그 외 → `Long IDENTITY` (외부 노출 시 Hashids 인코딩 필수)

`spring.jpa.open-in-view=false` — 트랜잭션 외부에서 lazy 로딩 금지. 서비스 레이어에서 필요한 연관관계는 명시적으로 fetch.

## 8. 인증 컨텍스트 — `@AuthenticationPrincipal CustomOAuth2User`

인증 필요한 컨트롤러는 사용자 정보를 다음과 같이 받는다:
```java
@GetMapping
public ApiResponse<...> list(@AuthenticationPrincipal CustomOAuth2User user) {
    UUID userId = user.getUserId();
    ...
}
```

`permitAll` 경로(예: `/api/v1/posts/**`)도 내부에서 `@AuthenticationPrincipal`로 사용자 식별 가능 (`JwtFilter`가 토큰 있으면 항상 컨텍스트 채움). 단, 토큰 없을 수 있으니 `user == null` 가능성 고려.

---

# 자주 하는 실수 체크리스트

- ❌ 컨트롤러에서 `ResponseEntity<XxxDto>` 직접 반환 → ✅ `ApiResponse<XxxDto>`로 래핑
- ❌ `throw new RuntimeException("...")` → ✅ `throw new JiburoException(ErrorCode.XXX)`
- ❌ `ErrorCode`만 추가하고 i18n 키 누락 → ✅ messages.properties + messages_en.properties 모두 추가
- ❌ Path variable을 `@PathVariable Long id`로 받기 → ✅ `String`으로 받아 `HashidsUtils.decode`
- ❌ 응답 DTO ID 필드를 `Long`으로 노출 → ✅ `String`(인코딩)으로 노출
- ❌ enum만 추가 / `data.sql`만 추가 → ✅ 양쪽 동시 추가
- ❌ 변경 메서드에 `log.info("...")` 직접 호출 → ✅ `@AuditLog(action = ...)`
- ❌ 트랜잭션 밖에서 lazy 필드 접근 → ✅ 서비스 안에서 fetch join 또는 명시적 로드

---

# 추가 참고 (관련 skill)

도메인별 세부 규약은 다음 skill 참고:
- [[jiburo-chat]] — STOMP/WebSocket, ChatRoom, ChatMessage
- [[jiburo-post]] — LostPost JSON `detail`, QueryDSL Haversine
- [[jiburo-user-auth]] — OAuth2 + JWT, JwtFilter
- [[jiburo-image]] — Cloudflare R2 presigned URL
- [[jiburo-notification]] — typeCode + args + targetId 패턴