# Figma 디자인 vs 백엔드 구현 갭 분석

> 분석 대상: [JIBURO_Web_Mobile](https://www.figma.com/design/Nz6hDq1LnXEUhu96HoEibY/JIBURO_Web_Mobile?node-id=5-16) (node-id: 5:16, Android_Mobile 캔버스)
> 분석일: 2026-08-29
> 방법: Figma MCP로 전체 레이어/텍스트 데이터를 읽고, 화면에 드러난 기능을 실제 jiburo-server 코드(컨트롤러/엔티티/DTO)와 1:1 대조

## 문서 구성

| 파일 | 내용 |
|---|---|
| [01-screen-inventory.md](./01-screen-inventory.md) | Figma 화면 구조 + 화면별 UI 기능/필드 인벤토리 |
| [02-backend-gap-analysis.md](./02-backend-gap-analysis.md) | 항목별 백엔드 구현 여부 대조 (코드 근거 포함) |

## 결론 요약

디자인에서 우려됐던 항목 대부분은 **이미 백엔드에 구현되어 있음**이 코드 확인으로 밝혀졌다. 실제로 손이 필요한 항목은 소수다.

### 실제 신규/보완 필요 (Gap)

| 우선순위 | 항목 | 상태 |
|---|---|---|
| High | **마이페이지 전용 API** — 프로필 조회/수정, 알림·위치 설정, 내 게시물, 내 활동, 회원탈퇴 | ✅ 구현 완료 (`UserController`, `/api/v1/users/me/**`) |
| High | **공지사항 / 1:1 문의** | 보류 — 백엔드 도메인 신설 vs 정적 페이지·외부 채널 대체 여부 기획 확인 필요 |
| Medium | **채팅방 음소거(mute)** — `ChatParticipant`/`ChatRoom`에 알림 on/off 필드·API 없음 | 미구현 |
| Medium | **XP/레벨 표시 체계** — 디자인은 `LV.1`, `XP 10/100` 형태의 명시적 레벨 시스템이나, 백엔드는 `activityScore` + `BadgeType`(등급)만 존재 | 부분 구현 — 명명·계산식 재검토 필요 |
| Low | **찜 목록 상태 필터** — `GET /api/v1/posts/liked`에 `찾는 중/찾기 완료` 서버 필터 파라미터 없음 (현재는 `Pageable`만 받음) | 부분 구현 |
| Low | **공유하기** — 게시글 상세 공유 버튼에 대응하는 딥링크/공유링크 생성 API 없음 (OS 네이티브 공유만으로 충분할 수도 있어 기획 확인 필요) | 미구현 — 기획 확인 필요 |

### 우려했으나 이미 구현되어 있음 (Gap 아님)

- 찜/저장(좋아요) 기능 — `POST /api/v1/posts/{id}/like`, `GET /api/v1/posts/liked` (`LostPostController`)
- "주변탐색" 기능 전체 — `domain/finder` (`FinderController`, `/api/v1/finder/sessions/**`) 8개 엔드포인트로 이미 구현됨. 디자인 용어는 "주변탐색", 코드 용어는 "finder"라 매칭이 어려웠을 뿐
- 채팅방 나가기 — `ChatRoomController.exitRoom()` (soft delete)
- 목록 정렬(거리순/최신순) — `LostPostRepositoryImpl`의 `NearbySortType`
- 사례금 필드 — `LostPost.reward`
- "무는지 여부" 필드 — `AnimalDetailDto.isBiting`

자세한 근거는 [02-backend-gap-analysis.md](./02-backend-gap-analysis.md) 참고.

### 확정 기능 아님 (참고만)

Figma 파일 내 기획 메모(모임=커뮤니티, 임시보호=게시글 기능, 상점=커스터마이징, 탐정=다마고찌식 성장)는 1.0/2.0 버전 구분이 있는 **로드맵 아이디어**이며 현재 화면 설계에 반영된 확정 요구사항이 아니다. 지금 백엔드 작업 대상으로 보지 않는다.
