package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.Storage;
import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.*;
import com.jiburo.server.domain.admin.service.AdminAlertService.Category;
import com.jiburo.server.global.monitoring.ServerErrorRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAlertMonitorTest {

    @Mock private AdminMonitoringService monitoringService;
    @Mock private AdminAlertService alertService;
    @Mock private AlertDeduplicator deduplicator;
    private final ServerErrorRecorder recorder = new ServerErrorRecorder();
    private AdminAlertMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new AdminAlertMonitor(monitoringService, recorder, alertService, deduplicator);
        lenient().when(deduplicator.claimActivation(anyString())).thenReturn(true);
        lenient().when(deduplicator.claimRecovery(anyString())).thenReturn(true);
    }

    private MonitoringResponse status(Level db, Double storagePercent) {
        Storage storage = storagePercent == null ? null : new Storage(1, 100, storagePercent, storagePercent >= 80);
        return new MonitoringResponse(LocalDateTime.now(), new Overall(Level.UP, List.of()),
                new ServerInfo("app-1", LocalDateTime.now(), 10, "17", List.of()),
                new Jvm(1, 100, 1, 10, 4, 1, 1),
                List.of(new Dependency("database", db, 1, null), new Dependency("redis", Level.UP, 1, null)),
                storage, new Activity(0, 0, 0), new Errors(0, 0, List.of()));
    }

    @Test
    void DB_장애는_시작과_복구에_한_번씩만_보낸다() {
        given(monitoringService.get()).willReturn(status(Level.DOWN, null), status(Level.DOWN, null), status(Level.UP, 10.0));

        monitor.check();
        monitor.check();
        monitor.check();

        verify(alertService, times(1)).send(eq(Category.SYSTEM), eq("push.admin.DB_DOWN"), any());
        verify(alertService, times(1)).send(eq(Category.SYSTEM), eq("push.admin.DB_DOWN.recovered"), any());
    }

    @Test
    void 다른_서버가_이미_보냈으면_보내지_않는다() {
        given(monitoringService.get()).willReturn(status(Level.DOWN, 10.0));
        given(deduplicator.claimActivation("DB_DOWN")).willReturn(false);

        monitor.check();

        verify(alertService, never()).send(eq(Category.SYSTEM), eq("push.admin.DB_DOWN"), any());
    }

    @Test
    void 저장소는_기준을_넘을_때마다_한_번씩_보낸다() {
        given(monitoringService.get()).willReturn(status(Level.UP, 85.0), status(Level.UP, 96.0), status(Level.UP, 96.0),
                status(Level.UP, 50.0), status(Level.UP, 82.0));

        for (int i = 0; i < 5; i++) {
            monitor.check();
        }

        verify(alertService, times(2)).send(eq(Category.STORAGE), eq("push.admin.STORAGE_80"), any());
        verify(alertService, times(1)).send(eq(Category.STORAGE), eq("push.admin.STORAGE_95"), any());
    }

    @Test
    void 최근_5분_오류가_기준_이상이면_급증_알림() {
        given(monitoringService.get()).willReturn(status(Level.UP, 10.0));
        for (int i = 0; i < 10; i++) {
            recorder.record("GET", "/x", new RuntimeException("boom"));
        }

        monitor.check();

        verify(alertService).send(eq(Category.SYSTEM), eq("push.admin.ERROR_SURGE"), eq("app-1"), eq(10L));
    }
}
