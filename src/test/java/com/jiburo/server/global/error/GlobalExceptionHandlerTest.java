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
}
