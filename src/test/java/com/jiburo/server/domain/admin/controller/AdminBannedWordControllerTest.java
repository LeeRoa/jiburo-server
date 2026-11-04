package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.AddResult;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.TestResult;
import com.jiburo.server.domain.admin.service.AdminBannedWordService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminBannedWordController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminBannedWordControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminBannedWordService adminBannedWordService;

    @Test
    void 일반_사용자는_금지어를_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/banned-words").with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_금지어를_추가한다() throws Exception {
        given(adminBannedWordService.add(any(), any())).willReturn(new AddResult(List.of(), List.of("바보")));

        mockMvc.perform(post("/api/v1/admin/banned-words").with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"words\":[\"바보\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skipped[0]").value("바보"));
    }

    @Test
    void 금지어_노출만_가진_관리자는_미리보기는_되고_추가는_403() throws Exception {
        var readOnly = withAdmin(AdminMenu.BANNED_WORD, AdminAccessLevel.READ);
        given(adminBannedWordService.test("바보")).willReturn(new TestResult("***", true));

        mockMvc.perform(get("/api/v1/admin/banned-words/test").param("text", "바보").with(readOnly))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.masked").value("***"));
        mockMvc.perform(post("/api/v1/admin/banned-words").with(readOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"words\":[\"바보\"]}"))
                .andExpect(status().isForbidden());
        then(adminBannedWordService).should(never()).add(any(), any());
    }
}
