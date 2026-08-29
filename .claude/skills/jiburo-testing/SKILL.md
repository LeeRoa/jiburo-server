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
- `SecurityMockMvcRequestPostProcessors` — SecurityContext 주입 (`@WithMockUser` 사용하지 않음)

---

## 테스트 위치 규칙

```
src/test/java/com/jiburo/server/
├── JiburoServerApplicationTests.java        # 스모크 테스트 (건드리지 말 것)
├── domain/
│   ├── post/
│   │   ├── service/LostPostServiceTest.java
│   │   ├── service/PostLikeServiceTest.java      # 기능 단위로 파일 분리 가능
│   │   └── controller/LostPostControllerTest.java
│   ├── chat/
│   │   ├── service/ChatRoomServiceTest.java
│   │   └── controller/ChatRoomControllerTest.java
│   └── finder/
│       └── service/FinderServiceTest.java
└── global/
    └── error/GlobalExceptionHandlerTest.java
```

- **도메인 패키지 구조를 그대로 미러링**해서 만든다.
- 기능이 많은 서비스는 **기능 단위로 파일을 분리**할 수 있다. (예: `LostPostServiceTest` + `PostLikeServiceTest` 둘 다 `LostPostServiceImpl` 테스트)

---

## 1. 서비스 단위 테스트 (`@ExtendWith(MockitoExtension.class)`)

스프링 컨텍스트 없이 Mockito만 사용. 가장 빠르고 독립적.

```java
@ExtendWith(MockitoExtension.class)
class FinderServiceTest {

    @InjectMocks
    private FinderServiceImpl finderService;   // 구체 클래스 (인터페이스 X)

    @Mock
    private FinderSessionRepository finderSessionRepository;

    @Mock
    private LostPostRepository lostPostRepository;

    @Mock
    private UserRepository userRepository;

    private UUID userId;
    private User user;
    private LostPost post;

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);   // HashidsUtils 초기화 필수

        userId = UUID.randomUUID();
        user = User.builder()
                .oauthId("google_test")
                .nickname("테스터")
                .email("test@test.com")
                .roleCode(RoleType.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);  // @GeneratedValue ID 세팅
    }

    @Test
    void 세션_생성_성공() {
        // given
        given(lostPostRepository.findById(1L)).willReturn(Optional.of(post));
        given(finderSessionRepository.save(any())).willReturn(activeSession);

        // when
        String result = finderService.createSession(userId, dto);

        // then
        assertThat(result).isEqualTo(HashidsUtils.encode(10L));
        then(finderSessionRepository).should().save(any());
    }

    @Test
    void 게시글_없으면_POST_NOT_FOUND() {
        // given
        given(lostPostRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> finderService.createSession(userId, dto))
            .isInstanceOf(JiburoException.class)
            .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode())
                .isEqualTo(ErrorCode.POST_NOT_FOUND));
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
| JiburoException 단언 | `assertThatThrownBy(...).isInstanceOf(JiburoException.class).satisfies(...)` |
| generic 타입 스터빙 | `doReturn(value).when(mock).method()` — `given()`은 타입 에러 발생 |

### ID 세팅 패턴

```java
// @GeneratedValue 엔티티의 ID는 직접 생성자로 세팅 불가 → ReflectionTestUtils 사용
ReflectionTestUtils.setField(entity, "id", 1L);
```

### JPA save() 후 ID 세팅이 필요한 경우

JPA는 `save()` 후 인자 엔티티에 ID를 세팅하지만 Mockito는 그렇지 않음.
반환값 ID가 필요한 서비스 로직(`session.getId()` 같은)은 `doAnswer`로 시뮬레이션:

```java
doAnswer(invocation -> {
    FinderSession s = invocation.getArgument(0);
    ReflectionTestUtils.setField(s, "id", 10L);
    return s;
}).when(finderSessionRepository).save(any(FinderSession.class));
```

### @Value 필드 주입

서비스의 `@Value` 필드는 `ReflectionTestUtils.setField`로 주입:

```java
ReflectionTestUtils.setField(imageStorageService, "bucketName", "test-bucket");
ReflectionTestUtils.setField(imageStorageService, "expirationMinutes", 15L);
```

### Redis 의존 서비스 특수 패턴

`RedisTemplate`의 generic 메서드는 `given()` 대신 `doReturn()` 사용.
Lenient 설정 필요:

```java
@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchHistoryServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOps;

    @BeforeEach
    void setUp() {
        doReturn(zSetOps).when(redisTemplate).opsForZSet();   // doReturn 사용
    }
}
```

---

## 2. 컨트롤러 슬라이스 테스트 (`@WebMvcTest`)

웹 레이어만 로드. 서비스는 `@MockitoBean`으로 교체.

### 기본 설정 (모든 컨트롤러 테스트에 공통)

```java
@WebMvcTest(
    controllers = FinderController.class,
    excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}  // 필수
)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})    // 필수
class FinderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // 테스트 대상 서비스
    @MockitoBean private FinderService finderService;

    // 아래 5개는 SecurityConfig 로드 시 항상 필요한 인프라 빈
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
}
```

> ⚠️ Spring Boot 3.4+에서는 `@MockBean` 대신 `@MockitoBean` 사용.

### BeforeEach 설정 (공통 패턴)

```java
private UUID userId;
private CustomOAuth2User principal;
private String encodedId;

@BeforeEach
void setUp() {
    new HashidsUtils("test-salt", 8);   // 컨트롤러에서 decode()가 필요하므로 필수
    userId = UUID.randomUUID();
    principal = new CustomOAuth2User(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    encodedId = HashidsUtils.encode(1L);
}
```

### 인증 — `CustomOAuth2User` 주입

**`@WithMockUser` 사용하지 않는다.** `PreAuthenticatedAuthenticationToken` + `withUser()` 헬퍼를 사용:

```java
private RequestPostProcessor withUser() {
    return SecurityMockMvcRequestPostProcessors.authentication(
        new PreAuthenticatedAuthenticationToken(principal, null, principal.getAuthorities())
    );
}
```

사용:
```java
mockMvc.perform(get("/api/v1/finder/sessions").with(withUser()))
```

### GET 요청 예시

```java
@Test
void 세션_목록_인증_200_반환() throws Exception {
    // given
    given(finderService.getMySessions(eq(userId), any(Pageable.class)))
        .willReturn(new SliceImpl<>(List.of(mockDto()), PageRequest.of(0, 10), false));

    // when & then
    mockMvc.perform(get("/api/v1/finder/sessions").with(withUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].statusCode").value("ACTIVE"));
}
```

### POST 요청 예시

```java
@Test
void 세션_생성_인증_200_반환() throws Exception {
    // given
    FinderSessionCreateRequestDto request = new FinderSessionCreateRequestDto(encodedId, 37.5, 127.0);
    given(finderService.createSession(eq(userId), any())).willReturn(encodedId);

    // when & then
    mockMvc.perform(post("/api/v1/finder/sessions")
            .with(withUser())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(encodedId));
}
```

### 비로그인 / 에러 응답 단언 패턴

```java
// 인증 필요 엔드포인트 비로그인 → 401
mockMvc.perform(get("/api/v1/finder/sessions"))
    .andExpect(status().isUnauthorized())
    .andExpect(jsonPath("$.success").value(false))
    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

// 잘못된 Hashids → 400
mockMvc.perform(get("/api/v1/finder/sessions/{id}", "INVALID_HASH").with(withUser()))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.success").value(false))
    .andExpect(jsonPath("$.errorCode").value("INVALID_IDENTIFIER"));

// JiburoException 발생 → 서비스에서 throw, HTTP 상태와 errorCode 검증
willThrow(new JiburoException(ErrorCode.FINDER_SESSION_NOT_FOUND))
    .given(finderService).getSession(any(), any());
mockMvc.perform(get("/api/v1/finder/sessions/{id}", encodedId).with(withUser()))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.errorCode").value("FINDER_SESSION_NOT_FOUND"));
```

### `ApiResponse` JSON 경로 요약

| 필드 | jsonPath |
|---|---|
| 성공 여부 | `$.success` |
| 에러 코드 문자열 | `$.errorCode` |
| 응답 데이터 | `$.data` |
| 응답 데이터 하위 필드 | `$.data.fieldName` |
| Slice/Page 배열 첫 번째 요소 | `$.data.content[0].fieldName` |
| 단순 배열 첫 번째 요소 | `$.data[0]` |

---

## 3. GlobalExceptionHandler 테스트 (standaloneSetup)

Security 없이 `MockMvcBuilders.standaloneSetup()` 사용:

```java
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())      // 내부 더미 컨트롤러
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @GetMapping("/error/{code}")
        public void trigger(@PathVariable String code) {
            throw new JiburoException(ErrorCode.valueOf(code));
        }
    }
}
```

---

## 4. 리포지토리 슬라이스 테스트 (`@DataJpaTest`)

JPA 레이어만 로드. H2 인메모리 DB 자동 교체.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class LostPostRepositoryTest {

    @Autowired
    private LostPostRepository lostPostRepository;

    @Autowired
    private TestEntityManager em;
}
```

> ⚠️ `@DataJpaTest`는 Haversine 식(`acos`, `radians` 등)을 H2에서 지원하지 않는다.
> `searchByRadius` / `searchByViewport` QueryDSL 메서드는 서비스 단위 테스트나 MariaDB 통합 테스트로 커버한다.

---

## 5. 통합 테스트 (`@SpringBootTest`)

전체 컨텍스트 로드. 속도가 느리므로 꼭 필요한 경우에만.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class IntegrationTest {
    // 실제 DB가 필요 → docker compose -f compose-dev.yaml up -d 먼저 실행
}
```

---

## 6. 테스트 명명 규칙

```
메서드명: 한글 상황 설명_기대 결과
예) 세션_생성_성공
    게시글_없으면_POST_NOT_FOUND
    자신의_게시글에_세션_생성하면_FINDER_CANNOT_FIND_OWN_POST
    비로그인_UNAUTHORIZED_반환
    유효하지_않은_해시_ID면_400_반환
```

- 영문 카멜케이스보다 **한글 스네이크케이스** 권장 (가독성)
- given/when/then 주석은 **한글**로
- 섹션 구분은 `// ───────── 메서드명 ─────────` 형식

---

## 7. 자주 하는 실수

- ❌ `@MockBean` 사용 → ✅ Spring Boot 3.4+는 `@MockitoBean` 사용
- ❌ `@WithMockUser` 사용 → ✅ `PreAuthenticatedAuthenticationToken` + `withUser()` 헬퍼 사용
- ❌ `@TestPropertySource(properties = "app.hashids.salt=...")` → ✅ `new HashidsUtils("test-salt", 8)` 직접 호출
- ❌ `@WebMvcTest`에서 `excludeAutoConfiguration` 미설정 → ✅ `OAuth2ClientAutoConfiguration.class` 제외 필수
- ❌ Security 인프라 빈 `@MockitoBean` 누락 → ✅ `JpaMetamodelMappingContext`, `JwtTokenProvider`, `CustomOAuth2UserService`, `OAuth2SuccessHandler`, `ClientRegistrationRepository` 5개 필수
- ❌ `@WebMvcTest`에서 서비스를 `@Autowired`로 주입 → ✅ `@MockitoBean` 사용
- ❌ `@InjectMocks`에 인터페이스 지정 → ✅ 구체 클래스(`XxxServiceImpl`) 지정
- ❌ `JiburoException` 발생 여부만 보고 `ErrorCode` 무시 → ✅ `.satisfies(ex -> assertThat(((JiburoException)ex).getErrorCode()).isEqualTo(...))` 까지 단언
- ❌ `@DataJpaTest`에서 Haversine 포함 메서드 테스트 → ✅ H2 미지원, 서비스 단위 테스트로 커버
- ❌ Redis generic 메서드에 `given()` 사용 → ✅ `doReturn().when()` 사용 + `@MockitoSettings(strictness = Strictness.LENIENT)`
- ❌ JPA save 후 ID가 필요한 로직에서 mock이 null 반환 → ✅ `doAnswer`로 ID 세팅 시뮬레이션

---

## 관련 skill

- [[jiburo-conventions]] — `ApiResponse`, `JiburoException`, `ErrorCode` 구조
- [[jiburo-post]] — `LostPost` 엔티티 빌더 패턴, QueryDSL 구조
- [[jiburo-user-auth]] — `CustomOAuth2User`, `JwtTokenProvider`, `SecurityConfig` 구조
