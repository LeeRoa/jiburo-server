---
name: jiburo-commit
description: Use when creating git commits or branches for jiburo-server — commit
  message format (type/scope/subject), branch naming convention, PR rules, or any
  git workflow question. Triggers include keywords like git commit, git branch, 커밋,
  브랜치, PR, 머지, squash, 커밋 메시지, 브랜치 이름, 작업 시작, 작업 완료, push.
---

# Jiburo Git 커밋 & 브랜치 가이드

상세 원본: `docs/git-commit-convention.md`, `docs/git-branch-convention.md`

---

## 커밋 메시지 형식

```
<type> (<scope>) : <subject>
```

scope가 없을 때는 괄호 생략:
```
<type> : <subject>
```

### type 목록

| type | 사용 시점 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 구조 개선 |
| `test` | 테스트 코드 추가/수정 |
| `docs` | 문서(README, docs/, 주석) 변경 |
| `chore` | 빌드, 설정, 의존성, 권한 등 기타 잡무 |
| `style` | 포맷, 세미콜론 등 비기능적 변경 |
| `perf` | 성능 개선 |
| `hotfix` | 운영 환경 긴급 패치 |

### scope 목록

도메인 단위로 작성. 횡단 관심사는 `global` 또는 생략.

| scope | 해당 작업 |
|---|---|
| `user` | 인증/인가, OAuth2, JWT, 회원 프로필 |
| `post` | 분실/제보 게시글, 지도 조회 |
| `chat` | 채팅방, STOMP, 메시지/읽음 처리 |
| `image` | R2 업로드, presigned URL, ImageMeta |
| `notification` | 알림 목록/읽음 |
| `global` | config, error, response, audit log, common code, util |
| `infra` | Docker, compose, CI/CD, 빌드 설정 |

### subject 규칙

- **한글**로 명확하게 (무엇이 어떻게 바뀌었는지)
- **명령형/현재형**: "추가했음" ❌ → "추가" ✅
- **마침표 금지**
- **50자 이내** 권장

### 예시

```
feat (post) : 지도 반경 기반 분실 게시글 조회 API 추가
fix (image) : presigned URL 만료 시간 5분 → 15분 조정
refactor (post) : gender Code → enum 형식으로 변경
test (post) : LostPostService 단위 테스트 추가
chore : gradlew 실행 권한 부여
docs : 커밋 컨벤션 문서 추가
```

---

## 본문(body) 포함 커밋 — HEREDOC 필수

긴 설명이 필요할 때는 HEREDOC으로 작성:

```bash
git commit -m "$(cat <<'EOF'
refactor (post) : LostPost.detail JSON 컬럼 매핑을 dialect-native 로 변경

Hibernate 6의 @JdbcTypeCode(SqlTypes.JSON) 를 사용해 수동 직렬화 로직 제거.
DTO 변환은 LostPostDetailConverter 가 일관되게 담당하도록 통일.

Closes #142
EOF
)"
```

본문 줄바꿈은 **72자** 권장. 푸터에 `Closes #이슈번호`, `BREAKING CHANGE:` 등 기재.

---

## 브랜치 네이밍

```
<type>/<scope>/<short-description>
```

- `short-description`은 **영문 소문자 케밥케이스** (`-`로 단어 구분)
- 한글, 공백, 언더스코어, 카멜케이스 금지
- 3~5단어 이내

```
feat/post/lost-post-radius-search
fix/image/file-code-enum-mapping
refactor/chat/stomp-handler-jwt-validation
test/post/lost-post-service-unit-test
chore/gradlew-exec-permission
docs/commit-convention
```

---

## 작업 흐름 (브랜치 → 커밋 → PR → 정리)

```bash
# 1. main 최신화 후 작업 브랜치 생성
git checkout main
git pull origin main
git checkout -b feat/post/lost-post-radius-search

# 2. 작업 후 커밋
git add <파일>
git commit -m "feat (post) : 지도 반경 기반 분실 게시글 조회 API 추가"

# 3. 원격 푸시
git push -u origin feat/post/lost-post-radius-search

# 4. PR 생성 후 Squash and merge

# 5. 머지 후 브랜치 삭제
git checkout main
git pull origin main
git branch -d feat/post/lost-post-radius-search
git push origin --delete feat/post/lost-post-radius-search
```

---

## PR 규칙

- **베이스 브랜치는 항상 `main`**
- 머지 방식은 **Squash and merge** — PR 제목 자체가 커밋 메시지가 되므로 본 컨벤션을 따를 것
- 머지 전 `main`을 rebase해 충돌 미리 해소 권장
- 머지된 작업 브랜치는 즉시 삭제

---

## 커밋 단위 원칙

- **논리적으로 하나의 변경**만 한 커밋에 담기 (`feat`와 `refactor` 섞지 않기)
- 포맷/빌드 자동 변경은 **별도 `chore` 커밋**으로 분리
- WIP/typo 커밋은 PR 머지 전 `git rebase -i`로 squash 정리

---

## 자주 하는 실수

- ❌ `git commit -m "수정"` — ✅ 무엇을 왜 수정했는지 명확히
- ❌ subject에 마침표 붙이기 → ✅ 마침표 없이
- ❌ `feat`에 `refactor` 내용 섞기 → ✅ 커밋 분리
- ❌ 브랜치명에 한글 또는 공백 → ✅ 영문 케밥케이스
- ❌ PR 제목을 컨벤션 무시하고 작성 → ✅ Squash merge 시 PR 제목이 그대로 커밋 메시지가 됨

---

## 관련 skill

- [[jiburo-conventions]] — 도메인 scope 구분 기준
- [[jiburo-testing]] — `test` type 커밋 작성 시
