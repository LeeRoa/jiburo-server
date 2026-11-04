package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.dto.AdminEmoticonDtos.PackResponse;
import com.jiburo.server.domain.admin.service.AdminEmoticonService;
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
        controllers = AdminEmoticonController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
class AdminEmoticonControllerTest extends AdminControllerTestSupport {

    @MockitoBean private AdminEmoticonService adminEmoticonService;

    private PackResponse pack() {
        return new PackResponse("abc123", "강아지", false, 0, List.of(), null);
    }

    @Test
    void 일반_사용자는_이모티콘_관리에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/emoticons/packs").with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자는_묶음을_생성한다() throws Exception {
        given(adminEmoticonService.createPack(any())).willReturn(pack());

        mockMvc.perform(post("/api/v1/admin/emoticons/packs").with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"강아지\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("abc123"))
                .andExpect(jsonPath("$.data.visible").value(false));
    }

    @Test
    void 묶음_이름이_비면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/emoticons/packs").with(withAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        then(adminEmoticonService).should(never()).createPack(any());
    }

    @Test
    void 이모티콘_노출만_가진_관리자는_조회는_되고_업로드_URL_발급은_403() throws Exception {
        var readOnly = withAdmin(AdminMenu.EMOTICON, AdminAccessLevel.READ);
        given(adminEmoticonService.findAll()).willReturn(List.of(pack()));

        mockMvc.perform(get("/api/v1/admin/emoticons/packs").with(readOnly))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("강아지"));
        mockMvc.perform(post("/api/v1/admin/emoticons/presigned-url").with(readOnly)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extension\":\"gif\",\"fileSize\":1000}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        then(adminEmoticonService).should(never()).presign(any(), any());
    }
}
