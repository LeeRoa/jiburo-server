---
name: jiburo-testing
description: Use when writing test code for jiburo-server — unit tests for services,
  controller slice tests (MockMvc), repository tests, security mock setup (CustomOAuth2User),
  JiburoException/ErrorCode assertions, ApiResponse structure validation, or any test
  under src/test/. Triggers include keywords like @Test, @WebMvcTest, @SpringBootTest,
  MockMvc, Mockito, @DataJpaTest, WithMockUser, assertThat, JiburoException, 테스트,
  단위 테스트, 통합 테스트, 컨트롤러 테스트, 슬라이스 테스트.
---

# Jiburo 테스트 코드 가이드

## 테스트 스택

`spring-boot-starter-test`에 다음이 모두 포함되어 있음 (추가 의존성 불필요):
- **JUnit 5** (JupiterAPI) — 테스트 프레임워크
- **Mockito** — 목(Mock) 객체
- **AssertJ** — `assertThat(...)` 체이닝 단언
- **Spring Test / MockMvc** — 컨트롤러 슬라이스 테스트
- **H2** (`runtimeOnly`) — 인메모리 DB (Repository/통합 테스트용)

`spring-security-test`:
- `@WithMockUser`, `SecurityMockMvcRequestPostProcessors` — SecurityContext 주입

## 테스트 위치 규칙

```
src/test/java/com/jiburo/server/
├── JiburoServerApplicationTests.java        # 스모크 테스트 (건드리지 말 것)
├── domain/
│   ├── post/
│   │   ├── service/LostPostServiceTest.java
│   │   ├── controller/LostPostControllerTest.java
│   │   └── repository/LostPostRepositoryTest.java
│   ├── chat/
│   │   └── service/ChatRoomServiceTest.java
│   └── ...
└── global/
    └── util/HashidsUtilsTest.java
```

**도메인 패키지 구조를 그대로 미러링**해서 만든다.

---

## 1. 서비스 단위 테스트 (`@ExtendWith(MockitoExtension.class)`)

스프링 컨텍스트 없이 Mockito만 사용. 가장 빠르고 독립적.

```java
@ExtendWith(MockitoExtension.class)
class LostPostServiceTest {

    @InjectMocks
    private LostPostServiceImpl lostPostService;

    @Mock
    private LostPostRepository lostPostRepository;

    @Mock
    private CommonCodeCache commonCodeCache;

    @Test
    void 게시글_조회_성공() {
        // given
        LostPost post = LostPost.builder()...build();
        given(lostPostRepository.findById(1L)).willReturn(Optional.of(post));

        // when
        LostPostResponseDto result = lostPostService.getPost(1L);

        // then
        assertThat(result.id()).isEqualTo(HashidsUtils.encode(1L));
    }

    @Test
    void 게시글_없으면_POST_NOT_FOUND_예외() {
        // given
        given(lostPostRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> lostPostService.getPost(999L))
            .isInstanceOf(JiburoException.class)
            .satisfies(ex -> {
                assertThat(((JiburoException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.POST_NOT_FOUND);
            });
    }
}
```

### 핵심 패턴

| 상황 | 코드 |
|---|---|
| 메서드 스터빙 | `given(mock.method(arg)).willReturn(value)` |
| void 메서드 예외 | `willThrow(new JiburoException(...)).given(mock).method(arg)` |
| 호출 검증 | `then(mock).should().method(arg)` |
| 호출 안 됨 검증 | `then(mock).should(never()).method(any())` |
| JiburoException 단언 | `assertThatThrownBy(...).isInstanceOf(JiburoException.class)` |

---

## 2. 컨트롤러 슬라이스 테스트 (`@WebMvcTest`)

웹 레이어만 로드. 서비스는 `@MockBean`으로 교체.

### 기본 설정

```java
@WebMvcTest(LostPostController.class)
@Import(SecurityConfig.class)   // SecurityConfig가 필요한 경우
class LostPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LostPostService lostPostService;

    @MockBean
    private HashidsUtils hashidsUtils;   // static 유틸이지만 @MockBean 대신 테스트용 salt 설정
}
```

> ⚠️ `@WebMvcTest`는 `@Component`, `@Service`, `@Repository`를 로드하지 않는다. 컨트롤러가 의존하는 모든 Bean은 `@MockBean`으로 선언해야 한다.

### 인증 — `CustomOAuth2User` 주입

`@AuthenticationPrincipal CustomOAuth2User`가 필요한 엔드포인트는 아래 헬퍼를 사용:

```java
// 테스트 헬퍼 (공통 추출 권장)
public static SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockUser(UUID userId) {
    return SecurityMockMvcRequestPostProcessors.oidcLogin()
        .idToken(token -> token.subject(userId.toString()))
        .userInfoToken(claims -> claims.claim("sub", userId.toString()));
}

// 또는 CustomOAuth2User를 직접 생성하는 경우
public static RequestPostProcessor withUser(UUID userId) {
    CustomOAuth2User principal = new CustomOAuth2User(...);
    return SecurityMockMvcRequestPostProcessors.user(principal);
}
```

간단히 `@WithMockUser`로 대체 가능한 경우:
```java
@Test
@WithMockUser   // 인증 여부만 필요할 때 (userId 불필요)
void 인증된_사용자_접근_허용() throws Exception { ... }
```

### GET 요청 예시

```java
@Test
void 게시글_단건_조회_200() throws Exception {
    // given
    String encodedId = HashidsUtils.encode(1L);
    LostPostResponseDto dto = new LostPostResponseDto(...);
    given(lostPostService.getPost(1L)).willReturn(dto);

    // when & then
    mockMvc.perform(get("/api/v1/posts/{id}", encodedId)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(encodedId));
}
```

### POST 요청 예시

```java
@Test
@WithMockUser
void 게시글_등록_201() throws Exception {
    // given
    LostPostCreateRequestDto request = new LostPostCreateRequestDto(...);
    given(lostPostService.createPost(any(), any())).willReturn(HashidsUtils.encode(1L));

    // when & then
    mockMvc.perform(post("/api/v1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
}
```

### 에러 응답 단언 패턴

```java
// JiburoException → ApiResponse.fail 변환 검증
mockMvc.perform(get("/api/v1/posts/{id}", "INVALID_HASH"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.success").value(false))
    .andExpect(jsonPath("$.errorCode").value("INVALID_IDENTIFIER"));
```

### `ApiResponse` JSON 경로 요약

| 필드 | jsonPath |
|---|---|
| 성공 여부 | `$.success` |
| HTTP 상태 코드 | `$.status` |
| 에러 코드 문자열 | `$.errorCode` |
| 에러 메시지 | `$.message` |
| 응답 데이터 | `$.data` |
| 응답 데이터 하위 필드 | `$.data.fieldName` |
| 배열 첫 번째 요소 | `$.data[0].fieldName` |

---

## 3. 리포지토리 슬라이스 테스트 (`@DataJpaTest`)

JPA 레이어만 로드. H2 인메모리 DB 자동 교체.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)  // H2로 교체
class LostPostRepositoryTest {

    @Autowired
    private LostPostRepository lostPostRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void 작성자로_게시글_조회() {
        // given
        User user = em.persist(User.builder()...build());
        LostPost post = em.persist(LostPost.builder().user(user)...build());
        em.flush();
        em.clear();

        // when
        List<LostPost> result = lostPostRepository.findByUserId(user.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(post.getId());
    }
}
```

> ⚠️ `@DataJpaTest`는 Haversine 식(`acos`, `radians` 등)을 H2에서 지원하지 않는다.  
> `searchByRadius` / `searchByViewport` QueryDSL 메서드는 **TestContainers(MariaDB)** 또는 서비스 단위 테스트로 커버한다.

### QueryDSL 테스트 시 추가 설정

```java
@DataJpaTest
@Import(QueryDslConfig.class)   // JPAQueryFactory 빈 등록
class LostPostRepositoryImplTest {

    @Autowired
    private EntityManager em;

    private LostPostRepositoryImpl repositoryImpl;

    @BeforeEach
    void setUp() {
        repositoryImpl = new LostPostRepositoryImpl(new JPAQueryFactory(em));
    }
}
```

---

## 4. 통합 테스트 (`@SpringBootTest`)

전체 컨텍스트 로드. 속도가 느리므로 꼭 필요한 경우에만.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LostPostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // 실제 DB가 필요 → compose-dev.yaml 기동 상태에서 실행하거나
    // @ActiveProfiles("test")로 H2 프로파일 별도 구성
}
```

> 실제 MariaDB가 필요한 통합 테스트는 `docker compose -f compose-dev.yaml up -d` 후 실행.

---

## 5. 프로젝트 특화 모킹 패턴

### CommonCodeCache 모킹

```java
@MockBean
private CommonCodeCache commonCodeCache;

// 특정 코드 있을 때
given(commonCodeCache.get("CATEGORY", "ANIMAL"))
    .willReturn(Optional.of(new CommonCode(...)));

// 없을 때 (캐시 미스 → JiburoException 유발)
given(commonCodeCache.get(anyString(), anyString()))
    .willReturn(Optional.empty());
```

### HashidsUtils — static 메서드

`HashidsUtils`는 static 메서드(`encode`/`decode`)를 사용하므로 **목 필요 없음**.  
테스트 환경에서 실제 salt 값이 없으면 `@TestPropertySource`로 주입:

```java
@TestPropertySource(properties = "app.hashids.salt=test-salt")
```

또는 `application-test.properties`에 등록:
```properties
app.hashids.salt=test-salt
```

### JwtTokenProvider / 필터 우회 (`@WebMvcTest`)

`@WebMvcTest`에서는 `JwtFilter`가 기본 비활성화.  
Security 설정까지 로드하려면 `@Import(SecurityConfig.class)` + `@MockBean JwtTokenProvider`:

```java
@WebMvcTest(LostPostController.class)
@Import(SecurityConfig.class)
class LostPostControllerTest {
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    ...
}
```

### AuditLog AOP (`@AuditLog`)

`@WebMvcTest`는 AOP를 로드하지 않아 `@AuditLog`가 동작하지 않는다. 어노테이션 동작 자체를 테스트하려면 `@SpringBootTest`로 전환.

---

## 6. 테스트 명명 규칙

```
메서드명: 한글 상황 설명_기대 결과
예) 게시글_없으면_POST_NOT_FOUND_예외
    유효하지_않은_해시_ID면_400반환
    작성자_아닌_사용자가_삭제하면_POST_ACCESS_DENIED
```

- 영문 카멜케이스보다 **한글 스네이크케이스** 권장 (가독성)
- given/when/then 주석은 **한글**로

---

## 7. 자주 하는 실수

- ❌ `@WebMvcTest`에서 서비스를 `@Autowired`로 주입 시도 → ✅ `@MockBean` 사용
- ❌ `@DataJpaTest`에서 Haversine 식 포함 메서드 테스트 → ✅ H2 미지원, 서비스 단위 테스트나 MariaDB 통합 테스트로 커버
- ❌ `@SpringBootTest`를 기본으로 남용 → ✅ 슬라이스 테스트(`@WebMvcTest`, `@DataJpaTest`)로 범위 최소화
- ❌ 컨트롤러 테스트에서 `HashidsUtils.decode`가 실패해 `INVALID_IDENTIFIER` 발생 → ✅ `@TestPropertySource(properties = "app.hashids.salt=...")`로 salt 주입
- ❌ `CustomOAuth2User`가 필요한 엔드포인트에 `@WithMockUser`만 사용 → ✅ userId가 필요하면 `SecurityMockMvcRequestPostProcessors.user(principal)` 사용
- ❌ `JiburoException` 발생 여부만 보고 `ErrorCode` 무시 → ✅ `getErrorCode()` 단언까지 함께 확인
- ❌ 통합 테스트를 로컬 MariaDB 없이 실행 → ✅ `compose-dev.yaml up` 먼저 실행

---

## 관련 skill

- [[jiburo-conventions]] — `ApiResponse`, `JiburoException`, `ErrorCode` 구조
- [[jiburo-post]] — `LostPost` 엔티티 빌더 패턴, QueryDSL 구조
- [[jiburo-user-auth]] — `CustomOAuth2User`, `JwtTokenProvider`, `SecurityConfig` 구조
