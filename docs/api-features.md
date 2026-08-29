# Jiburo Server — 구현된 기능 목록

> 마지막 업데이트: 2026-06-21

---

## 목차

- [Auth — 인증/인가](#auth--인증인가)
- [User — 마이페이지](#user--마이페이지)
- [Post — 분실 게시글](#post--분실-게시글)
- [Chat — 채팅](#chat--채팅)
- [Notification — 알림](#notification--알림)
- [Image — 이미지 업로드](#image--이미지-업로드)
- [Common Code — 공통 코드](#common-code--공통-코드)

---

## Auth — 인증/인가

### OAuth2 소셜 로그인
| 기능 | 방식 | 경로 | 인증 |
|---|---|---|---|
| Google 로그인 | OAuth2 Redirect | `/oauth2/authorization/google` | 불필요 |
| Kakao 로그인 | OAuth2 Redirect | `/oauth2/authorization/kakao` | 불필요 |
| Naver 로그인 | OAuth2 Redirect | `/oauth2/authorization/naver` | 불필요 |
| JWT 토큰 재발급 | `POST` | `/api/v1/auth/reissue` | 불필요 |

- 로그인 성공 시 `OAuth2SuccessHandler`가 Access Token / Refresh Token 발급 후 프론트 redirect URI로 전달
- Refresh Token은 Redis에 저장, Access Token은 응답 헤더/파라미터로 전달
- 이후 모든 요청은 `Authorization: Bearer {accessToken}` 헤더 필요

---

## User — 마이페이지

### REST API (`/api/v1/users/me`) — 전부 인증 필요

| 기능 | 메서드 | 경로 |
|---|---|---|
| 내 프로필 조회 | `GET` | `/api/v1/users/me` |
| 내 프로필 수정 (닉네임/이미지) | `PATCH` | `/api/v1/users/me` |
| 알림·위치 설정 조회 | `GET` | `/api/v1/users/me/settings` |
| 알림·위치 설정 변경 | `PATCH` | `/api/v1/users/me/settings` |
| 내 게시물 목록 | `GET` | `/api/v1/users/me/posts` |
| 내 활동(탐색 세션) 목록 | `GET` | `/api/v1/users/me/activities` |
| 회원 탈퇴 | `DELETE` | `/api/v1/users/me` |

> - `PATCH` 요청의 필드는 모두 optional — `null`이면 기존 값을 유지합니다.
> - 닉네임 중복 시 `DUPLICATE_NICKNAME`(409). 본인의 현재 닉네임을 그대로 보내면 중복 검사를 건너뜁니다.
> - `내 게시물`은 `/api/v1/posts/search`와 동일한 검색 조건(`statusCode`, `keyword` 등)을 지원하며, 작성자는 항상 로그인 사용자로 강제됩니다.
> - `내 활동`은 파인더 탐색 세션 이력(`/api/v1/finder/sessions`와 동일한 응답)입니다.

### 회원 탈퇴 정책 (soft delete)

```
DELETE /api/v1/users/me
  ├─ isDeleted = true, deletedAt = now
  ├─ oauthId → "withdrawn_{uuid}" (같은 소셜 계정으로 재가입 가능)
  ├─ nickname → "탈퇴한 사용자", email/profileImageUrl → null
  └─ 작성한 게시글 / 채팅 기록은 그대로 유지 (FK 무결성 보존)
```

탈퇴한 사용자는 `/api/v1/users/me` 계열 API에서 `USER_NOT_FOUND`(404)를 받습니다.

---

## Post — 분실 게시글

### REST API (`/api/v1/posts`)

| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| 게시글 등록 | `POST` | `/api/v1/posts` | 필요 |
| 게시글 검색/목록 (페이징) | `GET` | `/api/v1/posts/search` | 선택 (`mineOnly=true`면 필요) |
| 게시글 단건 조회 | `GET` | `/api/v1/posts/{id}` | 선택 |
| 게시글 수정 | `PATCH` | `/api/v1/posts/{id}` | 필요 |
| 게시글 상태 변경 | `PATCH` | `/api/v1/posts/{id}/status` | 필요 |
| 게시글 삭제 | `DELETE` | `/api/v1/posts/{id}` | 필요 |
| 좋아요 토글 | `POST` | `/api/v1/posts/{id}/like` | 필요 |
| 관심 게시글 목록 | `GET` | `/api/v1/posts/liked` | 필요 |
| 지도 viewport 마커 조회 | `GET` | `/api/v1/posts/map` | 불필요 |
| 반경 내 게시글 목록 (Haversine) | `GET` | `/api/v1/posts/nearby` | 불필요 |

### 게시글 상태 (`PostStatus`)
| 상태 | 설명 |
|---|---|
| `FINDING` | 찾는 중 |
| `COMPLETE` | 완료 |

### 카테고리 (`CategoryType`)
| 카테고리 | 설명 |
|---|---|
| `LOST` | 분실 게시글 (내가 잃어버린 동물) |
| `SIGHTED` | 제보 게시글 (목격 신고) |

---

## Chat — 채팅

### REST API (`/api/v1/chat/rooms`)

| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| 채팅방 생성 | `POST` | `/api/v1/chat/rooms` | 필요 |
| 내 채팅방 목록 조회 | `GET` | `/api/v1/chat/rooms` | 필요 |
| 채팅 메시지 내역 조회 (페이징) | `GET` | `/api/v1/chat/rooms/{roomId}/messages` | 필요 |
| 채팅 메시지 키워드 검색 | `GET` | `/api/v1/chat/rooms/{roomId}/messages/search` | 필요 |

### STOMP 실시간 채널 (`/ws-jiburo`)

| 방향 | 채널 | 설명 |
|---|---|---|
| 발행 (클라 → 서버) | `/pub/chat/rooms/{roomId}` | 메시지 전송 |
| 발행 (클라 → 서버) | `/pub/chat/rooms/{roomId}/read` | 읽음 처리 |
| 구독 (서버 → 클라) | `/sub/chat/rooms/{roomId}` | 실시간 메시지 수신 |
| 구독 (서버 → 클라) | `/sub/chat/rooms/{roomId}/read` | 읽음 영수증 수신 |

### 메시지 타입 (`ChatMsgType`)
| 타입 | 설명 |
|---|---|
| `TEXT` | 텍스트 메시지 |
| `IMAGE` | 이미지 메시지 (R2 URL) |

> - STOMP CONNECT 시 `StompHandler`가 JWT 검증
> - 채팅방은 게시글 단위 1:N 구조 (하나의 게시글에 여러 채팅방 가능)
> - 채팅방 목록은 `lastChatAt` 기준 내림차순 정렬

---

## Notification — 알림

### REST API (`/api/v1/notifications`)

| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| 내 알림 목록 조회 (무한스크롤) | `GET` | `/api/v1/notifications` | 필요 |
| 안 읽은 알림 개수 조회 | `GET` | `/api/v1/notifications/unread-count` | 필요 |

> **TODO**: 선택 알림 읽음 처리, 전체 읽음 처리, 선택 알림 삭제 미구현

---

## Image — 이미지 업로드

### REST API (`/api/v1/images`)

| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| Presigned PUT URL 발급 | `POST` | `/api/v1/images/presigned-url` | 필요 |
| 업로드 완료 콜백 | `POST` | `/api/v1/images/complete` | 필요 |

### 업로드 플로우
```
1. POST /api/v1/images/presigned-url  →  서버가 R2 presigned URL 발급 + ImageMeta(PENDING) 생성
2. PUT {presignedUrl}                 →  클라이언트가 R2에 직접 업로드
3. POST /api/v1/images/complete       →  서버가 ImageMeta 상태를 PENDING → COMPLETED 로 전환
```

### 업로드 대상 타입 (`UploadTargetType`)
| 타입 | 경로 prefix |
|---|---|
| `CHAT` | `chat/{userId}/` |
| `POST` | `post/{userId}/` |
| `PROFILE` | `profile/{userId}/` |

> - 전체 누적 용량 10GB 초과 시 업로드 불가
> - 키 구조: `{basePath}/{userId}/{uuid}.{ext}`

---

## Common Code — 공통 코드

### REST API (`/api/v1/common-codes`)

| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| 전체 공통 코드 조회 | `GET` | `/api/v1/common-codes` | 불필요 |
| 그룹별 코드 조회 | `GET` | `/api/v1/common-codes/{group}` | 불필요 |

> 앱 시작 시 1회 호출해 클라이언트에서 로컬 캐싱하는 용도
> 서버 내부에서도 `CommonCodeCache`로 메모리 캐싱 (DB JOIN 없이 조회)

---

## Search History — 검색 기록

> 로그인 유저 전용. Redis ZSet 기반 (최신순 최대 10개, 동일 항목 재검색 시 타임스탬프 갱신)

### 키워드 검색 기록 (`/api/v1/posts/search/history`)

| 기능 | 메서드 | 경로 | 비고 |
|---|---|---|---|
| 키워드 기록 목록 | `GET` | `/api/v1/posts/search/history` | 최신순 반환 |
| 전체 키워드 기록 삭제 | `DELETE` | `/api/v1/posts/search/history` | |
| 특정 키워드 기록 삭제 | `DELETE` | `/api/v1/posts/search/history/{keyword}` | URL 인코딩 필요 시 적용 |

> `/api/v1/posts/search` 호출 시 `keyword` 파라미터가 있으면 자동 저장

### 위치 검색 기록 (`/api/v1/posts/location-history`)

| 기능 | 메서드 | 경로 | 비고 |
|---|---|---|---|
| 위치 기록 목록 | `GET` | `/api/v1/posts/location-history` | `{lat, lng, radius, key}` 반환 |
| 전체 위치 기록 삭제 | `DELETE` | `/api/v1/posts/location-history` | |
| 특정 위치 기록 삭제 | `DELETE` | `/api/v1/posts/location-history/{key}` | `key` = `{lat4dp}_{lng4dp}_{radius1dp}` |

> `/api/v1/posts/nearby` 호출 시 로그인 유저라면 자동 저장
> 삭제 시 `key` 필드에 반환된 문자열 그대로 사용 (예: `37.5665_126.9780_3.0`)

---

## 전체 엔드포인트 요약

| 도메인 | 엔드포인트 수 | 인증 필요 |
|---|---|---|
| Auth | 4 (OAuth2 3 + REST 1) | 일부 |
| User (마이페이지) | 7 | 전체 |
| Post | 10 | 일부 |
| Chat (REST) | 4 | 전체 |
| Chat (STOMP) | 4채널 | 전체 (STOMP CONNECT 시 JWT) |
| Notification | 2 (미구현 3) | 전체 |
| Image | 2 | 전체 |
| Common Code | 2 | 불필요 |
| Search History | 6 | 전체 |
| **합계** | **37 REST + 4 STOMP채널** | |
