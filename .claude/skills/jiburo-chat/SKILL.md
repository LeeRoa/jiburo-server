---
name: jiburo-chat
description: Use when working on jiburo-server chat features — WebSocket/STOMP handlers,
  ChatRoom/ChatMessage/ChatParticipant entities, message send/read flow, chat room
  list/detail/search, or anything under domain/chat/. Triggers include keywords like
  ChatController, ChatRoomController, StompHandler, SimpMessagingTemplate,
  @MessageMapping, /ws-jiburo, /pub/chat, /sub/chat, ChatRoom, ChatMessage,
  ChatParticipant, lastChatAt, 채팅방, STOMP, WebSocket, 읽음 처리.
---

# Jiburo Chat 도메인 가이드

게시글(`LostPost`) 단위로 1:N 채팅방을 운영. REST(채팅방 CRUD) + STOMP(실시간 메시지)의 하이브리드 구조.

## 진입점 (Entry points)

| 종류 | 클래스 | 경로 |
|---|---|---|
| REST | `ChatRoomController` | `/api/v1/chat/rooms` |
| STOMP | `ChatController` | `@MessageMapping("/chat/rooms/{roomId}")` |

WebSocket 엔드포인트: `/ws-jiburo` (`WebSocketConfig`에서 등록, `permitAll`)
- 발행 prefix: `/pub`
- 구독 prefix: `/sub`

## 핵심 파일

- `domain/chat/controller/ChatController.java` — STOMP `@MessageMapping`
- `domain/chat/controller/ChatRoomController.java` — REST API
- `domain/chat/config/StompHandler.java` — `ChannelInterceptor`, CONNECT 시 JWT 검증
- `global/config/WebSocketConfig.java` — `/sub`, `/pub`, `/ws-jiburo` 등록
- `domain/chat/service/{ChatRoomService,ChatMessageService}Impl.java`
- `domain/chat/repository/Chat{Room,Message}Repository{,Custom,Impl}.java`
- `domain/chat/domain/{ChatRoom,ChatMessage,ChatParticipant}.java`
- `domain/chat/domain/enums/ChatMsgType.java` (`CommonCodeType` 구현)

## 엔티티 / DB 구조

```
ChatRoom (PK Long)
 ├── post_id → LostPost
 ├── host_id → User (방장)
 ├── last_message, last_chat_at  ← 목록 정렬 + 미리보기
 └── participants (1:N)
     └── ChatParticipant
         └── user_id, last_read_message_id

ChatMessage (PK Long)
 ├── chat_room_id → ChatRoom
 ├── sender_id → User
 ├── content (TEXT)
 └── message_type_code (ENUM: ChatMsgType)
```

핵심 인덱스:
- `idx_chatroom_post (post_id)` — 게시글별 채팅방 조회
- `idx_chatroom_last_chat (last_chat_at)` — **목록 정렬 최적화**
- `idx_chat_room_created (chat_room_id, created_at)` — **메시지 페이징 최적화의 핵심**

## STOMP 채널 / API 한눈에

### STOMP

| 방향 | 채널 | 페이로드 |
|---|---|---|
| 클라 → 서버 | `/pub/chat/rooms/{roomId}` | `ChatMessageRequestDto` |
| 클라 → 서버 | `/pub/chat/rooms/{roomId}/read` | `ChatReadDto.Request` (`lastReadMessageId` Hashids) |
| 서버 → 클라 | `/sub/chat/rooms/{roomId}` | `ChatMessageResponseDto` |
| 서버 → 클라 | `/sub/chat/rooms/{roomId}/read` | `ChatReadDto.Response` |

### REST (`/api/v1/chat/rooms`)

| Method | Path | 설명 |
|---|---|---|
| POST | `/` | 채팅방 생성 (`@AuditLog CHAT_ROOM_CREATE`) |
| GET | `/` | 내 채팅방 목록 (정렬: `last_chat_at` DESC) |
| GET | `/{roomId}/messages` | 메시지 내역 (`Pageable`, sort `createdAt ASC`, size 50) |
| GET | `/{roomId}/messages/search` | 키워드 검색 (`Slice`) |

## 도메인 규약 (Conventions)

1. **인증**: REST 컨트롤러는 `@AuthenticationPrincipal CustomOAuth2User user`, STOMP는 `Authentication authentication` 받아 `(CustomOAuth2User) authentication.getPrincipal()` 캐스팅.
2. **roomId / messageId는 Hashids String** — STOMP/REST 모두 `HashidsUtils.decode(roomId)`로 변환. 응답 DTO에서는 다시 `encode`.
3. **메시지 저장 직후 방 상태 갱신** — `chatMessageService.saveMessage(...)` 안에서 `ChatRoom.updateLastChat(message)`로 `lastMessage`/`lastChatAt`을 갱신해야 목록 정렬이 맞음.
4. **읽음 처리 흐름** — `ChatParticipant.lastReadMessageId`를 갱신 → 그 방의 다른 참여자에게 `/sub/.../read`로 영수증 push.
5. **새 메시지 타입 추가** — `ChatMsgType` enum + `data.sql`(`group_code='CHAT_TYPE'`) 동시 추가. [[jiburo-conventions]] §5 참고.
6. **양방향 연관관계 편의 메서드** — `ChatRoom.addParticipant(p)`를 통해 추가 (반대편 `participant.setChatRoom` 호출 포함).

## 자주 하는 실수 (Common pitfalls)

- ❌ STOMP 핸들러에서 메시지 저장은 했는데 `messagingTemplate.convertAndSend(...)`를 빼먹음 → 클라이언트가 받지 못함. 두 단계는 항상 짝.
- ❌ `convertAndSend` destination을 `/pub/...`로 적음 → ✅ **항상 `/sub/...`** (`/pub`은 인바운드 prefix)
- ❌ `last_chat_at` 갱신을 잊고 메시지만 저장 → 목록 정렬 깨짐
- ❌ STOMP CONNECT 시 `Authorization` 헤더 누락 → `StompHandler`가 토큰 없으면 통과시키지만 메서드에서 `authentication.getPrincipal()` 캐스팅 NPE
- ❌ `idx_chat_room_created` 없는 형태로 메시지 페이징 쿼리 작성 → 풀스캔
- ❌ 같은 사용자/방 조합으로 `ChatParticipant` 중복 생성 → `ALREADY_JOINED_ROOM` 던질 것

## ErrorCode (현재 등록된 것)

- `CHAT_ROOM_NOT_FOUND`
- `ALREADY_JOINED_ROOM`
- `CANNOT_CHAT_WITH_SELF`
- `NOT_CHAT_PARTICIPANT`
- `PARTNER_NOT_FOUND`

새 에러 추가는 [[jiburo-conventions]] §2 참고.

## 수동 테스트 페이지

`src/main/resources/test-web/` 아래에 STOMP 수동 테스트 HTML이 있음.

## 관련 skill

- [[jiburo-conventions]] — `ApiResponse`/Hashids/`@AuditLog` 등 공통 규약
- [[jiburo-user-auth]] — `StompHandler`가 사용하는 JWT 검증
- [[jiburo-post]] — `ChatRoom.post` 가 가리키는 `LostPost`