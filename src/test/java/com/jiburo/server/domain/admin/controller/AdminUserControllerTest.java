package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

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
        controllers = AdminUserController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminUserControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminUserService adminUserService;

    @Test
    void 사용자_메뉴_노출_권한으로_검색할_수_있다() throws Exception {
        given(adminUserService.search(any(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/users").param("adminOnly", "true")
                        .with(withAdmin(AdminMenu.USER, AdminAccessLevel.READ)))
                .andExpect(status().isOk());
    }

    @Test
    void 노출만_있으면_역할을_부여할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{id}/admin-roles", UUID.randomUUID())
                        .with(withAdmin(AdminMenu.USER, AdminAccessLevel.READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isForbidden());
        then(adminUserService).should(never()).updateRoles(any(), any(), any());
    }

    @Test
    void 편집_권한이_있으면_요청자_ID와_함께_서비스로_위임한다() throws Exception {
        UUID target = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/admin/users/{id}/admin-roles", target)
                        .with(withAdmin(AdminMenu.USER, AdminAccessLevel.WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[\"abc12345\"]}"))
                .andExpect(status().isOk());
        then(adminUserService).should().updateRoles(eq(adminId), eq(target), eq(List.of("abc12345")));
    }

    @Test
    void 사용자_ID_형식이_잘못되면_400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/{id}", "not-a-uuid").with(withAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TYPE_VALUE"));
    }
}
