---
name: jiburo-post
description: Use when working on jiburo-server lost/found post features — LostPost
  CRUD, status changes, map/radius search, JSON detail field handling, image list
  updates, or anything under domain/post/. Triggers include keywords like LostPostController,
  LostPost, LostPostImage, TargetDetailDto, AnimalDetailDto, LostPostDetailConverter,
  Haversine, QLostPost, viewport, radius, /api/v1/posts, CategoryType, PostStatus,
  분실, 제보, 게시글, 지도, 반경.
---

# Jiburo Post (LostPost) 도메인 가이드

분실/제보 게시글 도메인. 일반 CRUD 위에 **(1) 카테고리별 JSON 상세 컬럼**과 **(2) 지도 기반 조회(Viewport + Haversine 반경)** 두 가지 특수 패턴이 핵심.

## 진입점

`LostPostController` (`/api/v1/posts`) — `SecurityConfig`에서 **permitAll**이지만 컨트롤러 내부에서 `@AuthenticationPrincipal CustomOAuth2User` 사용 (없을 수도 있음).

| Method | Path | 설명 | AuditLog |
|---|---|---|---|
| POST | `/` | 등록 (응답: 인코딩된 ID String) | `POST_CREATE` |
| GET | `/search` | 통합 검색 (`Page`, dynamic) | - |
| GET | `/{id}` | 단건 조회 | - |
| PATCH | `/{id}` | 수정 | `POST_UPDATE` |
| PATCH | `/{id}/status` | 상태 변경 (`LOST` ↔ `COMPLETE`) | `POST_STATUS_CHANGE` |
| DELETE | `/{id}` | 삭제 | `POST_DELETE` |
| GET | `/map` | 지도 viewport 마커 (`List`) | - |
| GET | `/nearby` | 반경 리스트 (`Slice`, 거리순) | - |

## 핵심 파일

- `domain/post/controller/LostPostController.java`
- `domain/post/service/LostPostServiceImpl.java`
- `domain/post/repository/LostPostRepositoryImpl.java` ← **QueryDSL 동적 검색 + Haversine**
- `domain/post/domain/LostPost.java`, `LostPostImage.java`
- `domain/post/domain/enums/{CategoryType,PostStatus,VisibilityType,AnimalType,GenderType}.java`
- `domain/post/dto/detail/{TargetDetailDto,AnimalDetailDto,LostPostDetailConverter}.java`

## 엔티티 / DB 구조

```
LostPost (PK Long)
 ├── user_id → User (작성자)
 ├── finder_id → User (발견자, NULLABLE — 비회원 발견 가능)
 ├── category_code (ENUM CategoryType: ANIMAL/PERSON/ITEM/ETC)
 ├── status_code (ENUM PostStatus: LOST/COMPLETE)
 ├── visibility_code (ENUM VisibilityType: PUBLIC/PROTECTED/PRIVATE)
 ├── detail JSON  ← @JdbcTypeCode(SqlTypes.JSON), TargetDetailDto 인터페이스
 ├── latitude, longitude, found_location
 ├── lost_date, reward
 ├── image_url (대표)
 └── images (1:N) → LostPostImage (order로 순서 유지)
```

핵심 인덱스:
- `idx_lost_post_location (latitude, longitude)` — **지도/반경 조회의 핵심**
- `idx_lost_post_status (status_code)`
- `idx_lost_post_category (category_code)`

## 특수 패턴 1: 카테고리별 JSON 상세 (`detail`)

`detail`은 카테고리에 따라 구조가 바뀌는 JSON 컬럼. 인터페이스 + Jackson 다형성으로 자동 디스패치:

```java
// TargetDetailDto.java
@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnimalDetailDto.class, name = "ANIMAL")
    // 추후 PERSON, ITEM 추가
})
public interface TargetDetailDto {}
```

저장은 `LostPostDetailConverter`(JPA `AttributeConverter`)가 `ObjectMapper`로 String 직렬화 → MySQL `json` 컬럼. 실패 시 `INTERNAL_SERVER_ERROR` 던짐.

### 새 카테고리 detail DTO 추가 4단계

1. `dto/detail/XxxDetailDto.java` — `TargetDetailDto` 구현 (record 권장)
2. `TargetDetailDto`의 `@JsonSubTypes`에 `@Type(value = XxxDetailDto.class, name = "PERSON")` 추가
3. `CategoryType` enum에 해당 값이 이미 있는지 확인 (없으면 추가 + `data.sql` — [[jiburo-conventions]] §5)
4. (검색 필요 시) `LostPostRepositoryImpl.ALLOWED_JSON_KEYS`에 새 필드 키 추가

### JSON 동적 검색 — 화이트리스트 필수

`LostPostRepositoryImpl.buildJsonFilters`는 **`ALLOWED_JSON_KEYS` 화이트리스트 외 키를 무시**한다 (SQL injection / 의도치 않은 컬럼 검색 방지). 새 검색 필드 추가 시 화이트리스트도 같이 업데이트.

MySQL 검색 식: `JSON_UNQUOTE(JSON_EXTRACT(detail, '$.animalType')) = 'DOG'`

## 특수 패턴 2: 지도 조회 — Viewport + Radius (Haversine)

`LostPostRepositoryImpl.searchByRadius`:
1. **Bounding Box 1차 필터** — `radius / 111.0`로 위도/경도 ±도 범위 잡아 `idx_lost_post_location` 인덱스 태움
2. **Haversine 2차 정밀 필터** — `6371 * acos(...)` 식으로 km 단위 정확 거리 계산
3. **Slice + (size + 1)** — count 쿼리 없이 `hasNext` 판별
4. 거리 ASC 정렬

`searchByViewport`는 SW/NE 좌표로 마커만 가져옴 (limit 적용).

⚠️ Haversine 식은 MySQL/MariaDB 종속(`acos`, `radians`, `sin`, `cos` 함수). H2 등으로 DB를 바꾸면 식을 재작성해야 함.

## 도메인 규약

1. **외부 ID는 모두 Hashids String** — Path variable `String` → `HashidsUtils.decode`. 등록 응답도 `HashidsUtils.encode(postId)`.
2. **수정 시 부분 업데이트** — `LostPost.update(...)`가 null 체크로 받은 값만 갱신. `int reward`는 0 의미가 모호하므로 그냥 덮어씀.
3. **이미지 업데이트 최적화** — `LostPost.updateImages(urls)`는 **순서까지 같으면 무쿼리 종료**. 다르면 clear 후 재생성. 0번 인덱스가 대표(`imageUrl`).
4. **상태 변경은 `changeStatus(...)` 또는 `complete(finder, resultNote)`** — `complete()`는 finder가 null이어도 OK (비회원 발견).
5. **정렬 화이트리스트** — `ALLOWED_SORT_PROPERTIES = {createdAt, lostDate, reward}`만 허용. 기본 정렬 `createdAt DESC`.
6. **트랜잭션 안에서 작업자 권한 확인** — 수정/삭제 전 `lostPost.getUser().getId().equals(userId)` 검증 후 `POST_ACCESS_DENIED`.

## 자주 하는 실수

- ❌ 새 detail DTO 만들고 `@JsonSubTypes` 등록 안 함 → 역직렬화 시 `type` 못 찾아 NPE/InvalidTypeIdException
- ❌ JSON 검색 키 추가하면서 `ALLOWED_JSON_KEYS` 빼먹음 → 무시되어 조용히 결과 없음
- ❌ `searchByRadius`에서 Bounding Box 생략하고 Haversine만 → 풀스캔
- ❌ Path variable을 `@PathVariable Long id`로 → ✅ `String` + `HashidsUtils.decode`
- ❌ 수정 권한 확인 없이 바로 `lostPost.update(...)` → 다른 사람 글 수정됨
- ❌ `updateImages`에 같은 URL 같은 순서 보냈는데 매번 DELETE+INSERT 쿼리 도는 줄 알고 의심 → **순서까지 같으면 무쿼리**가 정상 동작
- ❌ `finder` 필드를 not-null로 가정하고 코드 작성 → nullable (비회원 발견)

## ErrorCode (현재 등록)

- `POST_NOT_FOUND` (404)
- `POST_ACCESS_DENIED` (403) — 작성자 불일치
- `POST_CATEGORY_INVALID` (400)
- `ANIMAL_NOT_FOUND`, `ANIMAL_STATUS_INVALID`, `REGION_NOT_SUPPORTED` (동물/지역 관련)

## 관련 skill

- [[jiburo-conventions]] — `ApiResponse`/Hashids/`@AuditLog` 등 공통 규약
- [[jiburo-image]] — `LostPostImage`에 들어가는 R2 업로드 플로우
- [[jiburo-chat]] — `LostPost` 1개에 대해 다수의 `ChatRoom` 생성됨