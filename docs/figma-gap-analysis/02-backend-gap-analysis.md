# 백엔드 구현 여부 대조

> [01-screen-inventory.md](./01-screen-inventory.md)에서 도출한 기능 후보를 실제 jiburo-server 코드와 대조한 결과. 코드 근거는 확인 시점(2026-08-29) 기준.

## 1. 찜/저장(좋아요) — ✅ 이미 구현됨

- `LostPostController.java:82-100` — `POST /api/v1/posts/{id}/like`(토글), `GET /api/v1/posts/liked`(내 찜 목록)
- `LostPostServiceImpl.java:163, 179` — `toggleLike`, `getLikedPosts`

**남는 갭**: `getLikedPosts(UUID userId, Pageable pageable)`는 상태(`FINDING`/`COMPLETE`) 필터 파라미터가 없다. 디자인의 `like_main` 화면은 "찾는 중/찾기 완료" 탭 필터를 요구하므로, 서버 필터 파라미터 추가가 필요할 수 있음(현재는 클라이언트가 전체 목록을 받아 직접 필터링해야 함). 다중선택 삭제도 개별 `toggleLike` 반복 호출로 대체 가능해 필수 신규 API는 아님.

## 2. XP/레벨 시스템 — ⚠️ 부분 구현

- `User.java:48-49` — `activityScore`(누적 활동 점수) 필드
- `User.java:74` — `increaseScore()`
- `User.java:79` — `updateBadge()`, `BadgeType` enum(등급)

디자인은 `LV.1`, `XP 10/100` 형태의 명시적 레벨·경험치 게이지와, 특정 행동에 `+50xp` 보상을 주는 UI를 노출한다. 백엔드는 "누적 점수 + 등급(Badge)" 방식만 있고 `xp`/`level`이라는 명명 체계나 레벨업 임계값 로직은 없다. 프론트가 `activityScore`를 자체적으로 레벨/XP로 환산하는 것인지, 별도 필드·로직이 필요한지 기획 확인 필요.

## 3. "주변탐색" — ✅ 이미 구현됨 (도메인명: `finder`)

`domain/finder/` 전체가 이 기능에 대응한다.

| 기능 | API |
|---|---|
| 탐색 세션 시작 | `POST /api/v1/finder/sessions` |
| 내 세션 목록 | `GET /api/v1/finder/sessions` |
| 세션 상세 | `GET /api/v1/finder/sessions/{sessionId}` |
| 촬영 미디어 추가/삭제 | `POST` / `DELETE /api/v1/finder/sessions/{sessionId}/media/{mediaId}` |
| 이동 경로(GPS) 기록/조회 | `POST` / `GET /api/v1/finder/sessions/{sessionId}/route` |
| 세션 제출(완료) | `POST /api/v1/finder/sessions/{sessionId}/submit` |
| 세션 취소 | `DELETE /api/v1/finder/sessions/{sessionId}` |

`FinderController.java` 전체, `FinderSession`/`FinderSessionMedia` 엔티티, `FinderResultType`/`FinderSessionStatus` enum까지 갖춰져 있다. 디자인 용어("주변탐색")와 코드 용어("finder")가 달라 처음엔 누락으로 보였을 뿐, 실제로는 이미 구현 완료.

## 4. 채팅방 음소거(mute) — ❌ 미구현

- `ChatParticipant.java`, `ChatRoom.java` 확인 결과 `isExited`(나감 여부), `lastReadMessageId`(읽음 처리)만 존재
- `mute`/`alarm`/`notification` on-off 관련 키워드 0건

디자인의 `chat_alarm` 화면("알림 끄기") 대응 기능이 백엔드에 없다. `ChatParticipant`에 `isMuted` 같은 필드 추가 + 알림 발송 로직에서 필터링하는 작업이 필요.

## 5. 채팅방 나가기 — ✅ 이미 구현됨

- `ChatRoomController.java:88` — `exitRoom()` (`@AuditLog(LogActionType.CHAT_ROOM_EXIT)`)
- `ChatRoomService.exitChatRoom(Long roomId, UUID userId)` → `ChatRoomServiceImpl.java:129-132`에서 `participant.exit()` 호출 (soft delete)

## 6. 마이페이지 — ❌ 대부분 미구현

- `UserController` 자체가 존재하지 않음. `user/controller`에는 `AuthController`(`/api/v1/auth/reissue` 등)만 있음
- `User.updateProfile()`(`User.java:83`)은 OAuth 로그인 시 소셜 프로필 동기화 전용으로만 호출됨(`AuthServiceImpl.java:122`, `CustomOAuth2UserService.java:61`) — 사용자가 직접 호출하는 프로필 수정 API 없음
- `notice`/`inquiry`/`withdraw`(공지사항/1:1문의/회원탈퇴) 키워드 검색 0건

디자인의 `mypage_main` 및 하위 화면(프로필 수정, 내 게시물/내 활동 전체보기, 계정관리, 약관, 회원탈퇴)에 대응하는 백엔드가 통째로 빠져 있다. 이 프로젝트에서 **가장 큰 갭**이며, 다음 하위 기능이 필요:

- 닉네임/프로필 이미지 수정 API
- 회원탈퇴(계정 삭제, soft/hard 결정 필요) API
- 공지사항 목록 (또는 정적 페이지로 대체 가능한지 기획 확인)
- 1:1 문의하기 (또는 외부 채널 연동으로 대체 가능한지 기획 확인)
- 알림/위치 설정 조회·변경 API
- 레벨·XP, 내 게시물, 내 활동 요약 조회 API (내 게시물은 `/api/v1/posts/search`에 `userId` 필터로 우회 가능한지 확인 필요, 내 활동은 신규 개념일 가능성)

## 7. 목록 정렬(거리순/최신순) — ✅ 이미 구현됨

- `LostPostRepositoryImpl.java:127-129` — `NearbySortType.LATEST` → `createdAt.desc()`, 그 외 → 거리 오름차순
- `ALLOWED_SORT_PROPERTIES`(45-46행: `createdAt`, `lostDate`, `reward` 포함) + `getOrderSpecifiers(Sort sort)`(157행)로 일반 목록 정렬도 지원

## 8. 사례금(현상금) 필드 — ✅ 이미 구현됨

- `LostPost.java:93-94` — `private int reward;`
- `LostPostCreateRequestDto.java:46`, `LostPostUpdateRequestDto.java:43`, `LostPostResponseDto.java:32`

## 9. "무는지 여부" 필드 — ✅ 이미 구현됨

- `AnimalDetailDto.java:20` — `Boolean isBiting` ("무는지 여부 (제보자 안전 안내용)")
- `LostPostCreateRequestDto.java:37`, `LostPostUpdateRequestDto.java:35`에도 동일 필드

## 10. 공유하기 — ❌ 미구현

`domain` 전체에서 `share`/`deeplink`/`공유` 키워드 검색 0건. 게시글 상세의 공유 아이콘이 OS 네이티브 공유 시트(현재 URL을 그냥 공유)로 충분한지, 아니면 서버가 단축 링크/딥링크를 발급해야 하는지는 기획 확인이 필요. 후자라면 신규 API 필요.

---

## 참고: 확정 요구사항이 아닌 로드맵 메모

Figma 파일 내 기획 인사이트 메모에 "모임=커뮤니티", "임시보호=게시글 기능", "상점=커스터마이징", "탐정=다마고찌식 성장" 등이 1.0/2.0 버전 구분과 함께 적혀 있으나, 이는 향후 로드맵 아이디어이며 현재 화면 설계에 반영된 확정 기능이 아니다. 지금 백엔드 작업 범위에 포함하지 않는다.
