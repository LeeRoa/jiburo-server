package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminSettingResponseDto;
import com.jiburo.server.domain.admin.service.AdminSettingService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.setting.SettingKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminSettingController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminSettingControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminSettingService adminSettingService;

    private AdminSettingResponseDto dto(String value) {
        return AdminSettingResponseDto.of(SettingKey.POST_MAX_IMAGES, value, null);
    }

    @Test
    void 일반_사용자는_설정을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings").with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_설정_목록을_조회한다() throws Exception {
        given(adminSettingService.findAll()).willReturn(List.of(dto("5")));

        mockMvc.perform(get("/api/v1/admin/settings").with(withAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].key").value("POST_MAX_IMAGES"))
                .andExpect(jsonPath("$.data[0].defaultValue").value("5"))
                .andExpect(jsonPath("$.data[0].max").value(20.0));
    }

    @Test
    void 관리자는_설정을_변경한다() throws Exception {
        given(adminSettingService.update(eq("POST_MAX_IMAGES"), eq("8"), any())).willReturn(dto("8"));

        mockMvc.perform(put("/api/v1/admin/settings/POST_MAX_IMAGES").with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"8\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("8"));
    }

    @Test
    void 운영_설정_노출만_가진_관리자는_조회는_되고_변경은_403() throws Exception {
        given(adminSettingService.findAll()).willReturn(List.of(dto("5")));
        var readOnly = withAdmin(com.jiburo.server.domain.admin.domain.AdminMenu.SETTING,
                com.jiburo.server.domain.admin.domain.AdminAccessLevel.READ);

        mockMvc.perform(get("/api/v1/admin/settings").with(readOnly))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/admin/settings/POST_MAX_IMAGES").with(readOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"8\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        org.mockito.BDDMockito.then(adminSettingService).should(org.mockito.Mockito.never()).update(any(), any(), any());
    }

    @Test
    void 운영_설정_메뉴가_없는_관리자는_조회도_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings").with(withAdmin(
                        com.jiburo.server.domain.admin.domain.AdminMenu.I18N,
                        com.jiburo.server.domain.admin.domain.AdminAccessLevel.WRITE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 잘못된_값이면_400() throws Exception {
        given(adminSettingService.update(eq("POST_MAX_IMAGES"), eq("999"), any()))
                .willThrow(new JiburoException(ErrorCode.INVALID_SETTING_VALUE));

        mockMvc.perform(put("/api/v1/admin/settings/POST_MAX_IMAGES").with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SETTING_VALUE"));
    }
}
