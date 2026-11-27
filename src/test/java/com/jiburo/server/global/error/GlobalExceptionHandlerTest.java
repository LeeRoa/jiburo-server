package com.jiburo.server.global.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.global.config.MessageSourceConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ── 더미 컨트롤러 ──────────────────────────────────────────────────────────

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/error/{code}")
        public void triggerJiburoException(@PathVariable String code) {
            throw new JiburoException(ErrorCode.valueOf(code));
        }

        @GetMapping("/server-error")
        public void triggerServerError() {
            throw new RuntimeException("unexpected");
        }

        record TestRequest(@NotBlank String name) {}

        @PostMapping("/validation")
        public void triggerValidationError(@Valid @RequestBody TestRequest request) {}

        enum Color { RED, BLUE }

        record EnumRequest(Color color) {}

        @PostMapping("/enum-body")
        public void enumBody(@RequestBody EnumRequest request) {}

        @GetMapping("/enum-param")
        public void enumParam(@RequestParam Color color) {}

        record SearchCondition(@NotBlank String keyword) {}

        @GetMapping("/model-attribute")
        public void modelAttribute(@Valid @ModelAttribute SearchCondition condition) {}
    }

    // ── JiburoException 처리 ──────────────────────────────────────────────────

    @Test
    void NOT_FOUND_에러코드는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/test/error/USER_NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void UNAUTHORIZED_에러코드는_401을_반환한다() throws Exception {
        mockMvc.perform(get("/test/error/UNAUTHORIZED"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void FORBIDDEN_에러코드는_403을_반환한다() throws Exception {
        mockMvc.perform(get("/test/error/ACCESS_DENIED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void CONFLICT_에러코드는_409를_반환한다() throws Exception {
        mockMvc.perform(get("/test/error/DUPLICATE_NICKNAME"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NICKNAME"));
    }

    @Test
    void BAD_REQUEST_에러코드는_400을_반환한다() throws Exception {
        mockMvc.perform(get("/test/error/INVALID_IDENTIFIER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_IDENTIFIER"));
    }

    // ── 일반 예외 처리 ────────────────────────────────────────────────────────

    @Test
    void 예상치_못한_예외는_500을_반환한다() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

    // ── Validation 예외 처리 ──────────────────────────────────────────────────

    @Test
    void 유효성_검사_실패는_400을_반환한다() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    // ── 클라이언트 입력 오류가 500이 아닌 4xx로 응답되는지 ─────────────────────────

    @Test
    void 깨진_JSON은_400() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{name:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 존재하지_않는_enum_값을_본문으로_보내면_400() throws Exception {
        mockMvc.perform(post("/test/enum-body").contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"GREEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 쿼리_파라미터_타입이_안_맞으면_400() throws Exception {
        mockMvc.perform(get("/test/enum-param").param("color", "GREEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TYPE_VALUE"));
    }

    @Test
    void 필수_쿼리_파라미터_누락은_400() throws Exception {
        mockMvc.perform(get("/test/enum-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void ModelAttribute_검증_실패는_400() throws Exception {
        mockMvc.perform(get("/test/model-attribute").param("keyword", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 지원하지_않는_메서드는_405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/test/server-error"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 지원하지_않는_Content_Type은_415() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.TEXT_PLAIN).content("name=a"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void 권한_부족_AccessDeniedException은_403() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AccessDeniedController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @RestController
    static class AccessDeniedController {
        @GetMapping("/denied")
        public void denied() {
            throw new org.springframework.security.access.AccessDeniedException("no");
        }
    }
}
