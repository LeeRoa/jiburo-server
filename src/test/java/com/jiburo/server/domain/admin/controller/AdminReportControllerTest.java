package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.dto.AdminReportDtos.ResolveResult;
import com.jiburo.server.domain.admin.service.AdminReportService;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminReportController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminReportControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminReportService adminReportService;

    @Test
    void 일반_사용자는_신고_목록을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports").with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_신고를_처리한다() throws Exception {
        given(adminReportService.resolve(eq(adminId), any(), any())).willReturn(new ResolveResult(ReportStatus.DISMISSED, 2, null));

        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", com.jiburo.server.global.util.HashidsUtils.encode(1L)).with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deleteContent\":false,\"suspendPermanent\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISMISSED"))
                .andExpect(jsonPath("$.data.handledCount").value(2));
    }

    @Test
    void 정지_일수가_범위를_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", com.jiburo.server.global.util.HashidsUtils.encode(1L)).with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspendDays\":0}"))
                .andExpect(status().isBadRequest());
        then(adminReportService).should(never()).resolve(any(), any(), any());
    }

    @Test
    void 신고_노출만_가진_관리자는_조회는_되고_처리는_403() throws Exception {
        var readOnly = withAdmin(AdminMenu.REPORT, AdminAccessLevel.READ);
        given(adminReportService.search(any(), any())).willReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/reports").with(readOnly))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", com.jiburo.server.global.util.HashidsUtils.encode(1L)).with(readOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
