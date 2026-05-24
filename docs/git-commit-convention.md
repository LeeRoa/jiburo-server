# Git 커밋 메시지 컨벤션

Jiburo 서버 저장소에서 사용하는 Git 커밋 메시지 컨벤션입니다.
브랜치 네이밍 컨벤션([git-branch-convention.md](./git-branch-convention.md))과 동일한 `type`/`scope` 어휘를 공유합니다.

## 1. 기본 형식

```
<type> (<scope>) : <subject>
```

- `type` — 작업의 성격 (필수)
- `scope` — 변경이 일어나는 도메인/모듈 (선택, 도메인 작업이면 권장)
- `subject` — 변경 내용을 요약한 한 줄 (한글 권장)

`scope`가 없을 때는 괄호를 생략합니다.

```
<type> : <subject>
```

### 예시

```
feat (post) : 지도 반경 기반 분실 게시글 조회 API 추가
fix (image) : ImageMeta.fileCode @Enumerated(EnumType.STRING) 누락 수정
refactor (post) : gender Code -> enum 형식으로 변경
chore : gradlew 실행 권한 부여
docs : 도메인별 작업 가이드 스킬 추가
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
| `hotfix`   | 운영 환경 긴급 패치                              |

## 3. scope 목록

도메인 모듈 단위로 작성합니다. 횡단 관심사는 `global` 또는 생략합니다.

- `user` — 인증/인가, OAuth2, JWT, 회원 프로필
- `post` — 분실/제보 게시글, 지도 조회
- `chat` — 채팅방, STOMP, 메시지/읽음 처리
- `image` — R2 업로드, presigned URL, ImageMeta
- `notification` — 알림 목록/읽음
- `global` — config, error, response, audit log, common code, util
- `infra` — Docker, compose, CI/CD, 빌드 설정

## 4. subject 작성 규칙

- **한글로 명확하게** — 무엇이 어떻게 바뀌었는지 한 줄로 요약
- **명령형/현재형**: "추가했음" 대신 "추가" / "수정", "개선"
- **마침표 금지**, 끝에 구두점을 붙이지 않음
- **50자 이내** 권장 (Git 도구에서 잘리지 않도록)
- 어떤(what) + 왜(why) 가 한 줄에 압축되면 가장 좋음 — `why`가 길어지면 본문(body)에 작성

### 좋은 예 / 나쁜 예

| Good                                                       | Bad                                  |
|------------------------------------------------------------|--------------------------------------|
| `feat (chat) : 안 읽은 메시지 카운트 STOMP 영수증 추가`        | `채팅 수정함.`                        |
| `fix (image) : presigned URL 만료 시간 5분 → 15분 조정`       | `버그 수정`                           |
| `refactor (global) : ErrorCode i18n 키 prefix 통일`         | `리팩토링`                            |
| `chore : gradlew 실행 권한 부여`                              | `update`                              |

## 5. 본문(body) / 푸터(footer) 작성

한 줄 subject로 충분하지 않을 때 본문을 추가합니다.

```
<type> (<scope>) : <subject>

<본문: 왜 이렇게 바꿨는지, 어떤 영향이 있는지 설명. 72자 줄바꿈 권장>

<푸터: BREAKING CHANGE, Closes #이슈번호 등>
```

### 예시

```
refactor (post) : LostPost.detail JSON 컬럼 매핑을 dialect-native 로 변경

Hibernate 6의 @JdbcTypeCode(SqlTypes.JSON) 를 사용해 수동 직렬화 로직 제거.
DTO 변환은 LostPostDetailConverter 가 일관되게 담당하도록 통일.

Closes #142
```

## 6. 커밋 단위 가이드

- **논리적으로 하나의 변경**만 한 커밋에 담기 — `feat`와 `refactor`를 섞지 말 것
- 빌드/포맷 자동 변경은 **별도 `chore` 커밋**으로 분리
- 머지 전 `git rebase -i`로 WIP/typo 커밋은 정리(squash)할 것
- PR 머지는 **Squash and merge** 가 기본이므로 PR 제목 자체가 곧 본 컨벤션을 따라야 함

## 7. 빠른 참고

```bash
# 일반 커밋
git commit -m "feat (chat) : STOMP 인바운드 채널 JWT 검증 추가"

# 스코프 없는 커밋
git commit -m "chore : gradle wrapper 8.5 -> 8.6 업그레이드"

# 본문 포함 커밋 (HEREDOC)
git commit -m "$(cat <<'EOF'
refactor (image) : ImageStorageServiceImpl 의 10GB 용량 가드 책임 분리

기존 upload 메서드에 섞여 있던 누적 용량 계산 로직을
QuotaPolicy 빈으로 추출해 단위 테스트 가능하도록 변경.
EOF
)"
```
