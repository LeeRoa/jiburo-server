package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.global.cache.CommonCodeCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminCommonCodeController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminCommonCodeControllerTest extends AdminControllerTestSupport {

    @MockitoBean private CommonCodeCache commonCodeCache;

    @Test
    void 비로그인은_401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/common-codes/reload"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        then(commonCodeCache).should(never()).reload();
    }

    @Test
    void 일반_사용자는_403_JSON() throws Exception {
        mockMvc.perform(post("/api/v1/admin/common-codes/reload").with(withUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        then(commonCodeCache).should(never()).reload();
    }

    @Test
    void 관리자는_캐시를_리로드할_수_있다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/common-codes/reload").with(withAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        then(commonCodeCache).should().reload();
    }
}
