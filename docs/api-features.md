# Jiburo Server — 구현된 기능 목록

> 마지막 업데이트: 2026-11-04

---

## 목차

- [Auth — 인증/인가](#auth--인증인가)
- [Post — 분실 게시글](#post--분실-게시글)
- [Chat — 채팅](#chat--채팅)
- [Notification — 알림](#notification--알림)
- [Image — 이미지 업로드](#image--이미지-업로드)
- [Common Code — 공통 코드](#common-code--공통-코드)
- [Admin — 관리자](#admin--관리자)

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

## Post — 분실 게시글

### REST API (`/api/v1/posts`)

| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| 게시글 등록 | `POST` | `/api/v1/posts` | 필요 |
| 게시글 검색/목록 (페이징) | `GET` | `/api/v1/posts/search` | 선택 |
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
| 채팅 이모티콘 목록 | `GET` | `/api/v1/chat/emoticons` | 필요 |

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
| `TALK` | 텍스트 메시지 (최대 길이: 운영 설정 `CHAT_MAX_TEXT_LENGTH`) |
| `IMAGE` | 이미지 메시지 (수신 content = R2 URL) |
| `VIDEO` | 동영상 메시지 (수신 content = R2 URL) |
| `MAP` | 지도/위치 정보 |
| `EMOTICON` | 이모티콘. 전송 content = 이모티콘 ID, 수신 content = 이미지 URL |
| `SYS` | 시스템 메시지 (입장, 퇴장 등) |

> - STOMP CONNECT 시 `StompHandler`가 JWT 검증
> - 채팅방은 게시글 단위 1:N 구조 (하나의 게시글에 여러 채팅방 가능)
> - 채팅방 목록은 `lastChatAt` 기준 내림차순 정렬 (텍스트 외 타입은 미리보기가 `[IMAGE]`, `[EMOTICON]` 형태)
> - 텍스트 메시지의 금지어는 `***`로 가려 저장·전송 (관리자 금지어 메뉴에서 관리)
> - 이모티콘은 관리자가 등록한 무료 이모티콘(png / 움직이는 gif·webp). 노출 중인 묶음만 목록·전송 가능하며, 비노출·삭제돼도 이미 보낸 메시지는 그대로 보임

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
| `CHAT` | `chat/images/` |
| `CHAT_VIDEO` | `chat/videos/` |
| `POST` | `post/images/` |
| `PROFILE` | `profile/images/` |
| `FINDER_IMAGE` | `finder/images/` |
| `FINDER_VIDEO` | `finder/videos/` |
| `EMOTICON` | `emoticon/images/` — 관리자 API로만 발급 |

> - 파일 1개 최대 크기·전체 누적 용량은 운영 설정(`UPLOAD_*`)으로 관리
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
> 각 코드의 `label`은 `Accept-Language` 기준 표시 문구 (관리자 수정값 → 파일 기본값 → ko). 서버에 문구가 없으면 생략되므로 프론트 번역으로 대체

---

## Admin — 관리자

모든 경로는 `/api/v1/admin` 아래이며 `ROLE_ADMIN` + 메뉴별 권한(READ=조회, WRITE=편집)이 필요합니다. 상태 변경 API는 감사 로그에 남습니다.

| 메뉴 | 주요 경로 | 기능 |
|---|---|---|
| (공통) | `GET /me` | 내 역할·메뉴 권한 |
| 대시보드 `DASHBOARD` | `GET /dashboard` | 기간별 가입·게시글·채팅방 집계, 저장소 사용량, 최근 관리자 작업 |
| 운영 설정 `SETTING` | `GET·PUT·DELETE /settings/{key}` | 업로드·채팅·검색·인증 제한값 조회/변경/기본값 복원 |
| 공통코드 `COMMON_CODE` | `POST /common-codes/reload` | 공통코드 캐시 다시 읽기 |
| 다국어 문구 `I18N` | `GET /i18n/locales`, `GET /i18n/messages`, `PUT·DELETE /i18n/messages/{key}/{locale}` | 문구 검색(번역 누락·수정 필터), 수정/추가, 기본값 되돌리기 |
| 사용자 `USER` | `GET /users`, `PUT /users/{userId}/admin-roles` | 사용자 검색, 관리자 역할 부여/회수 |
| 게시글 `POST` | `GET /posts`, `POST /posts/{postId}/force-delete·restore` | 전체 조회(삭제·비공개 포함), 강제 삭제(사유 필수), 복구 |
| 금지어 `BANNED_WORD` | `GET·POST /banned-words`, `PATCH·DELETE /banned-words/{wordId}`, `GET /banned-words/test` | 금지어 검색·추가·수정·삭제(등록자/수정자·시각 표시), 가리기 미리보기 |
| 이모티콘 `EMOTICON` | `/emoticons/packs/**`, `POST /emoticons/presigned-url` | 묶음 CRUD·순서, 이모티콘 업로드·등록·순서·삭제 |
| 감사 로그 `AUDIT_LOG` | `GET /audit-logs` | 사용자·액션·기간 검색 |
| 역할·권한 `ROLE` | `GET /menus`, `/roles/**` | 역할 CRUD, 역할별 메뉴 권한 (SUPER_ADMIN 전용 메뉴) |

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
| Post | 10 | 일부 |
| Chat (REST) | 5 | 전체 |
| Chat (STOMP) | 4채널 | 전체 (STOMP CONNECT 시 JWT) |
| Notification | 2 (미구현 3) | 전체 |
| Image | 2 | 전체 |
| Common Code | 2 | 불필요 |
| Admin | 메뉴 10개 (위 표) | 관리자 |
| Search History | 6 | 전체 |
| **합계** | **31 REST + 4 STOMP채널 (관리자 API 제외)** | |
