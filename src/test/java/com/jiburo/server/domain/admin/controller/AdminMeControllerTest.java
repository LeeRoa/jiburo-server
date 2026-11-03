package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.service.AdminAuthorizationService.AdminAccess;
import com.jiburo.server.domain.admin.service.AdminAuthorizationService.RoleSummary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminMeController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminMeControllerTest extends AdminControllerTestSupport {

    @Test
    void 내_역할로_노출되는_메뉴만_정의_순서대로_내려준다() throws Exception {
        AdminAccess access = new AdminAccess(false,
                List.of(new RoleSummary(3L, "TRANSLATOR", "번역 담당", false)),
                Map.of(AdminMenu.I18N, AdminAccessLevel.WRITE, AdminMenu.COMMON_CODE, AdminAccessLevel.READ));
        given(adminAuthorizationService.getAccess(adminId)).willReturn(access);

        mockMvc.perform(get("/api/v1/admin/me").with(withAdmin(AdminMenu.I18N, AdminAccessLevel.WRITE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.superAdmin").value(false))
                .andExpect(jsonPath("$.data.roles[0].code").value("TRANSLATOR"))
                .andExpect(jsonPath("$.data.menus.length()").value(2))
                .andExpect(jsonPath("$.data.menus[0].menu").value("COMMON_CODE"))
                .andExpect(jsonPath("$.data.menus[0].canWrite").value(false))
                .andExpect(jsonPath("$.data.menus[1].menu").value("I18N"))
                .andExpect(jsonPath("$.data.menus[1].canWrite").value(true));
    }

    @Test
    void 관리자_역할이_없으면_403() throws Exception {
        // ROLE_ADMIN 토큰이지만 실제 관리자 역할이 회수된 경우 (토큰 만료 전)
        given(adminAuthorizationService.hasAnyRole(adminId)).willReturn(false);

        mockMvc.perform(get("/api/v1/admin/me").with(withAdminTokenOnly()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}
