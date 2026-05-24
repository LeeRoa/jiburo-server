# Git 브랜치 생성 규칙

Jiburo 서버 저장소에서 사용하는 Git 브랜치 네이밍 컨벤션입니다.
커밋 메시지 컨벤션(`type (scope) : subject`)과 동일한 어휘를 사용해 일관성을 유지합니다.

## 1. 기본 형식

```
<type>/<scope>/<short-description>
```

- `type` — 작업의 성격 (필수)
- `scope` — 변경이 일어나는 도메인/모듈 (선택, 도메인 작업이면 권장)
- `short-description` — 케밥케이스(kebab-case) 영문 짧은 설명

### 예시

```
feat/post/lost-post-radius-search
fix/image/file-code-enum-mapping
refactor/chat/stomp-handler-jwt-validation
chore/gradle-wrapper-permission
docs/branch-convention
```

## 2. type 목록

| type       | 사용 시점                                      |
|------------|------------------------------------------------|
| `feat`     | 새로운 기능 추가                                |
| `fix`      | 버그 수정                                      |
| `refactor` | 동작 변경 없는 코드 구조 개선                    |
| `docs`     | 문서(README, docs/, 주석) 변경                  |
| `chore`    | 빌드, 설정, 의존성, 권한 등 기타 잡무             |
| `test`     | 테스트 코드 추가/수정                            |
| `style`    | 포맷, 세미콜론 등 비기능적 변경                  |
| `perf`     | 성능 개선                                      |
| `hotfix`   | 운영 환경 긴급 패치 (main에서 분기)              |

## 3. scope 목록

도메인 모듈 단위로 작성합니다. 횡단 관심사는 `global` 또는 생략합니다.

- `user` — 인증/인가, OAuth2, JWT, 회원 프로필
- `post` — 분실/제보 게시글, 지도 조회
- `chat` — 채팅방, STOMP, 메시지/읽음 처리
- `image` — R2 업로드, presigned URL, ImageMeta
- `notification` — 알림 목록/읽음
- `global` — config, error, response, audit log, common code, util
- `infra` — Docker, compose, CI/CD, 빌드 설정

## 4. base 브랜치 규칙

- 모든 작업 브랜치는 **`main`에서 분기**하고 PR 머지 후 삭제합니다.
- 운영 긴급 패치도 `main`에서 `hotfix/...`로 분기합니다.
- 머지 전 `main`을 rebase 하여 충돌을 미리 해소하는 것을 권장합니다.

## 5. short-description 작성 규칙

- 영문 소문자 + 하이픈(`-`)으로 단어 구분 (kebab-case)
- 한글, 공백, 언더스코어(`_`), 카멜케이스 사용 금지
- 가능하면 **3~5단어 이내**, 50자 이내
- 이슈 트래커가 있다면 끝에 번호를 붙여도 무방 (`feat/post/map-search-#123`)

### 좋은 예 / 나쁜 예

| Good                                  | Bad                                |
|---------------------------------------|------------------------------------|
| `feat/chat/last-chat-at-sort`         | `feat/chat/채팅방정렬`              |
| `fix/image/file-code-enum`            | `fix/imageBugFix`                   |
| `refactor/post/gender-code-to-enum`   | `refactor_post_gender`              |
| `chore/gradlew-exec-permission`       | `temp-branch`                       |

## 6. 머지 / 정리

- PR은 **Squash and merge** 를 기본으로 합니다 (커밋 히스토리 단순화).
- 머지된 작업 브랜치는 즉시 삭제합니다(`git push origin --delete <branch>`).
- 장기간 미사용 브랜치는 분기별로 정리합니다.

## 7. 빠른 참고

```bash
# 새 기능 작업 시작
git checkout main
git pull origin main
git checkout -b feat/post/lost-post-radius-search

# 작업 후 푸시
git push -u origin feat/post/lost-post-radius-search

# 머지 후 로컬/원격 정리
git checkout main
git pull origin main
git branch -d feat/post/lost-post-radius-search
git push origin --delete feat/post/lost-post-radius-search
```
