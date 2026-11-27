package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.Level;
import com.jiburo.server.global.monitoring.ServerErrorRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdminMonitoringServiceIntegrationTest {

    @Autowired private AdminMonitoringService adminMonitoringService;
    @Autowired private ServerErrorRecorder serverErrorRecorder;

    @Test
    void DB_상태와_JVM_저장소_최근_오류를_한_번에_돌려준다() {
        long before = serverErrorRecorder.totalSinceStart();
        serverErrorRecorder.record("GET", "/api/v1/test", new IllegalStateException("테스트 오류"));

        var res = adminMonitoringService.get();

        assertThat(res.dependencies()).extracting("name").containsExactly("database", "redis");
        assertThat(res.dependencies().get(0).level()).isEqualTo(Level.UP);
        assertThat(res.jvm().heapUsedBytes()).isPositive();
        assertThat(res.jvm().processors()).isPositive();
        assertThat(res.server().uptimeSeconds()).isGreaterThanOrEqualTo(0);
        assertThat(res.storage()).isNotNull();
        assertThat(res.errors().totalSinceStart()).isEqualTo(before + 1);
        assertThat(res.errors().recent().get(0).path()).isEqualTo("/api/v1/test");
        assertThat(res.errors().lastHour()).isPositive();
        // 최근 1시간 오류가 있으므로 정상(UP)이 아니고 사유에 표시된다
        assertThat(res.overall().level()).isNotEqualTo(Level.UP);
        assertThat(res.overall().reasons()).anyMatch(r -> r.contains("서버 오류"));
    }

    @Test
    void 최근_오류는_50건까지만_보관한다() {
        ServerErrorRecorder recorder = new ServerErrorRecorder();
        for (int i = 0; i < 60; i++) {
            recorder.record("GET", "/p" + i, new RuntimeException("x".repeat(400)));
        }
        assertThat(recorder.recent()).hasSize(50);
        assertThat(recorder.recent().get(0).path()).isEqualTo("/p59");
        assertThat(recorder.recent().get(0).message()).hasSize(301);
        assertThat(recorder.totalSinceStart()).isEqualTo(60);
    }
}
