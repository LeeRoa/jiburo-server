package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos.LogResponse;
import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos.SearchCondition;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.log.AuditLog;
import com.jiburo.server.global.log.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAuditLogServiceIntegrationTest {

    @Autowired private AdminAuditLogService service;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;

    private User admin;
    private final UUID ghostId = UUID.randomUUID(); // 탈퇴 등으로 users에 없는 사용자

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder().oauthId("kakao_admin").nickname("관리자").roleCode(RoleType.ADMIN).build());
        auditLogRepository.save(AuditLog.builder().userId(admin.getId()).action(LogActionType.ADMIN_SETTING_UPDATE).targetData("POST_MAX_IMAGES, value=8").clientIp("1.1.1.1").build());
        auditLogRepository.save(AuditLog.builder().userId(admin.getId()).action(LogActionType.ADMIN_ROLE_CREATE).targetData("TRANSLATOR").clientIp("1.1.1.1").build());
        auditLogRepository.save(AuditLog.builder().userId(ghostId).action(LogActionType.AUTH_LOGIN).targetData("Login").clientIp("2.2.2.2").build());
    }

    private Page<LogResponse> search(SearchCondition c) {
        return service.search(c, PageRequest.of(0, 20));
    }

    @Test
    void 액션_접두사로_관리자_작업만_조회한다() {
        Page<LogResponse> result = search(new SearchCondition(null, null, "admin_", null, null));

        assertThat(result.getContent()).extracting(LogResponse::action)
                .containsExactlyInAnyOrder(LogActionType.ADMIN_SETTING_UPDATE, LogActionType.ADMIN_ROLE_CREATE);
        assertThat(result.getContent()).allSatisfy(r -> assertThat(r.nickname()).isEqualTo("관리자"));
    }

    @Test
    void 사용자_정확한_액션_기간으로_조회하고_없는_사용자는_닉네임_null() {
        assertThat(search(new SearchCondition(admin.getId(), LogActionType.ADMIN_ROLE_CREATE, null, null, null)).getContent())
                .singleElement().satisfies(r -> assertThat(r.targetData()).isEqualTo("TRANSLATOR"));

        LogResponse ghost = search(new SearchCondition(ghostId, null, null, LocalDate.now(), LocalDate.now())).getContent().get(0);
        assertThat(ghost.nickname()).isNull();

        assertThat(search(new SearchCondition(null, null, null, LocalDate.now().plusDays(1), null)).getContent()).isEmpty();
        assertThat(search(new SearchCondition(null, null, "NOPE_", null, null)).getContent()).isEmpty();
    }

    @Test
    void 시작일이_종료일보다_늦으면_400() {
        assertThatThrownBy(() -> search(new SearchCondition(null, null, null, LocalDate.now(), LocalDate.now().minusDays(1))))
                .isInstanceOf(JiburoException.class);
    }
}
