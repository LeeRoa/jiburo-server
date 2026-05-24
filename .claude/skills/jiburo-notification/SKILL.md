---
name: jiburo-notification
description: Use when working on jiburo-server notification features — listing my
  notifications, unread count, read/delete (TODO), or anything under domain/notification/.
  Important: notification message text is NOT stored in DB — only typeCode + args
  (comma-separated) + targetId. Frontend composes the localized string. Triggers
  include keywords like NotificationController, Notification, NotificationResponseDto,
  NotificationServiceImpl, typeCode, args, targetId, /api/v1/notifications, unread-count,
  알림, 알람.
---

# Jiburo Notification 도메인 가이드

알림 목록 / 안 읽음 카운트. **메시지 본문을 서버 DB에 저장하지 않는다** — `typeCode`(번역 키) + `args`(가변 인자) + `targetId`(이동 대상)만 저장하고 **프론트가 i18n 키로 조립**.

## 진입점

`NotificationController` (`/api/v1/notifications`) — 인증 필요.

| Method | Path | 설명 |
|---|---|---|
| GET | `/` | 내 알림 목록 (`Slice`, sort `createdAt DESC`, size 20) |
| GET | `/unread-count` | 안 읽은 알림 개수 (`long`) |
| TODO | PATCH `/read` (선택/전체) | 읽음 처리 |
| TODO | DELETE | 알림 삭제 |

## 핵심 파일

- `domain/notification/controller/NotificationController.java`
- `domain/notification/service/NotificationService.java` + `Impl`
- `domain/notification/domain/Notification.java`
- `domain/notification/repository/NotificationRepository.java`
- `domain/notification/dto/{NotificationResponseDto,NotificationCreateRequestDto,NotificationReadRequestDto,NotificationDeleteRequestDto}.java`

## 엔티티 / DB

```
Notification (PK Long)
 ├── receiver_id → User (NOT NULL)
 ├── sender_id → User (NULLABLE — 시스템 알림)
 ├── type_code (30)            ← 예: "NOTI_CHAT", "NOTI_POST_LIKE"
 ├── args (255)                ← 콤마 구분 ("닉네임,게시글제목")
 ├── target_id (Long)          ← 클릭 시 이동할 대상 (게시글/방 PK, Hashids 인코딩 전)
 └── is_read (boolean)
```

인덱스:
- `idx_noti_receiver (receiver_id)` — 내 알림 조회
- `idx_noti_created_at (created_at)` — 정렬

## 핵심 패턴: typeCode + args + targetId

**서버는 사람이 읽는 메시지 문자열을 만들지 않는다.** 데이터만 내려보내고 프론트가 i18n 키 `typeCode`로 번역 + `args` 치환.

예시:
```json
{
  "id": "aB7x9YqZ",
  "typeCode": "NOTI_CHAT",
  "args": ["민지", "강아지 찾아요"],
  "targetId": 123,
  "isRead": false,
  "createdAt": "..."
}
```
→ 프론트 i18n: `noti.chat = "{0}님이 '{1}'에 메시지를 보냈습니다"` → `"민지님이 '강아지 찾아요'에 메시지를 보냈습니다"`

### args 직렬화
- DB: `","`로 join한 단일 `String` 컬럼
- 응답 DTO: `String.split(",")` → `List<String>`
- ⚠️ **인자 값에 콤마(`,`)가 들어가면 분리가 깨진다**. 닉네임/제목에 콤마가 가능하면 다른 구분자(`|`, ``)로 바꾸거나 JSON으로 저장.

### targetId 주의
- **현재 응답에 `Long` 그대로 노출됨** (`NotificationResponseDto.targetId`).
- 다른 도메인 ID는 모두 Hashids 인코딩하는데 여기는 raw Long. [[jiburo-conventions]] §3과 충돌. 도메인 일관성을 위해 Hashids 인코딩으로 바꾸는 게 좋음.

## 도메인 규약

1. **메시지 본문은 서버에서 만들지 않는다** — `typeCode`만 약속하고 i18n 등록은 프론트 책임 (또는 `i18n/common/messages*.properties`에 등록 후 프론트가 동일 키 참조).
2. **`sender`는 nullable** — 시스템 알림 (예: 공지) 가능성.
3. **알림 생성은 외부 도메인(chat/post 등)에서 호출** — `NotificationService.create(...)`로 묶어 한 곳에서 일관 처리하는 게 안전.
4. **읽음 처리는 부분 갱신** — 미구현 (TODO). 구현 시 `is_read=false`인 행만 update 효율적.
5. **새 `typeCode` 추가** — 단순 문자열이라 enum 강제는 없지만, 오타/누락 방지를 위해 `NotificationType` enum 도입 검토 (현재 없음). 도입한다면 [[jiburo-conventions]] §5의 `CommonCodeType` 패턴 따라 `data.sql`도 동기.
6. **Slice 응답** — `Page` 아닌 `Slice` 사용. count 쿼리 안 도는 대신 totalCount 없음.

## 자주 하는 실수

- ❌ 서버에서 사람이 읽는 문자열을 만들어 저장 → ✅ `typeCode + args`만
- ❌ args에 콤마 포함 가능한 값 (제목, 닉네임 변경 후 등) → 분리 시 깨짐. 별도 구분자/JSON로 변경
- ❌ `target_id`를 Hashids 인코딩 없이 사용 (현재 코드 상태) → 외부 노출 일관성 깨짐. 응답 DTO에서 `HashidsUtils.encode` 적용 검토
- ❌ `unread-count`를 매 페이지 진입마다 폴링 → 부하. WebSocket push로 알림 발생 시 클라이언트가 ++ 하는 방식이 일반적 (현재 미구현)
- ❌ `findByReceiverIdOrderByCreatedAtDesc`로 페이징 시 `created_at` 동률에 PK 보조 정렬 없음 → 페이지 경계에서 중복/누락 가능. 보조 정렬 `id DESC` 추가 권장
- ❌ 알림 생성과 본 작업(메시지 저장 등)을 **같은 트랜잭션**에 묶어 알림 생성 실패로 본 작업까지 롤백 → 분리(`@Async` 이벤트) 검토

## ErrorCode

- 현재 알림 전용 에러 코드 없음. 필요 시 `NOTIFICATION_NOT_FOUND`, `NOTIFICATION_ACCESS_DENIED` 등 추가 — [[jiburo-conventions]] §2.

## 관련 skill

- [[jiburo-conventions]] — `ApiResponse`, Hashids(특히 targetId 인코딩 이슈), `@AuditLog`
- [[jiburo-chat]] — 채팅 메시지 발생 시 `NOTI_CHAT` 생성 트리거
- [[jiburo-post]] — 게시글 좋아요/댓글 등 활동 시 알림 발생 트리거
- [[jiburo-user-auth]] — 수신자 식별 (`CustomOAuth2User.getUserId()`)
