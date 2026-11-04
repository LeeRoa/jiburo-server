package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.LocaleValue;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.MessageRow;
import com.jiburo.server.domain.admin.service.AdminI18nService;
import com.jiburo.server.global.i18n.MessageCatalog.Source;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminI18nController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminI18nControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminI18nService adminI18nService;

    private MessageRow row(String message) {
        return new MessageRow("error.post.not_found",
                List.of(new LocaleValue("ko", message, Source.DB, "기본", null, null)));
    }

    @Test
    void 일반_사용자는_문구를_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/i18n/locales").with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 점이_들어간_키와_언어_태그로_수정한다() throws Exception {
        given(adminI18nService.update(eq("error.post.not_found"), eq("zh-CN"), eq("새 문구"), any()))
                .willReturn(row("새 문구"));

        mockMvc.perform(put("/api/v1/admin/i18n/messages/error.post.not_found/zh-CN").with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"새 문구\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("error.post.not_found"))
                .andExpect(jsonPath("$.data.values[0].source").value("DB"));
    }

    @Test
    void 다국어_노출만_가진_관리자는_조회는_되고_수정은_403() throws Exception {
        var readOnly = withAdmin(AdminMenu.I18N, AdminAccessLevel.READ);
        given(adminI18nService.find("error.post.not_found")).willReturn(row("기본"));

        mockMvc.perform(get("/api/v1/admin/i18n/messages/error.post.not_found").with(readOnly))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("error.post.not_found"));
        mockMvc.perform(put("/api/v1/admin/i18n/messages/error.post.not_found/ko").with(readOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"새 문구\"}"))
                .andExpect(status().isForbidden());
        then(adminI18nService).should(never()).update(any(), any(), any(), any());
    }
}
