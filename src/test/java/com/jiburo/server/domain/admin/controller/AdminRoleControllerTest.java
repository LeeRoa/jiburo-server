package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.service.AdminRoleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminRoleController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminRoleControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminRoleService adminRoleService;

    @Test
    void 메뉴_정의_목록은_AdminMenu_순서대로_내려준다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/menus").with(withAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(AdminMenu.values().length))
                .andExpect(jsonPath("$.data[0].menu").value("DASHBOARD"))
                .andExpect(jsonPath("$.data[0].writable").value(false));
    }

    @Test
    void 다른_메뉴_편집_권한이_있어도_역할_메뉴_권한이_없으면_역할을_볼_수도_바꿀_수도_없다() throws Exception {
        var userManager = withAdmin(AdminMenu.USER, AdminAccessLevel.WRITE);

        mockMvc.perform(get("/api/v1/admin/roles").with(userManager))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/roles/{id}/menus", "abc12345").with(userManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menus\":[{\"menu\":\"SETTING\",\"read\":true,\"write\":true}]}"))
                .andExpect(status().isForbidden());
        then(adminRoleService).should(never()).updateMenus(any(), any());
    }

    @Test
    void SUPER_ADMIN은_역할_목록을_조회한다() throws Exception {
        given(adminRoleService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/roles").with(withAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
