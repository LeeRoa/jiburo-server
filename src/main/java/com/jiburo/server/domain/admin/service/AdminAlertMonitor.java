package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.Dependency;
import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.Level;
import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.MonitoringResponse;
import com.jiburo.server.domain.admin.service.AdminAlertService.Category;
import com.jiburo.server.global.monitoring.ServerErrorRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 주기적으로 시스템을 점검해 운영자 푸시를 보낸다.
 *
 * - 장애(DB·Redis 연결 실패, 5분간 서버 오류 급증): 시작될 때 1번, 복구될 때 1번. 계속 장애여도 반복하지 않는다.
 * - 저장소 80% / 95%: 넘을 때 1번. 기준 아래로 내려가면 다시 넘을 때 또 보낸다 (복구 알림 없음).
 * - 이 서버가 본 상태는 메모리에, 서버 간 중복은 AlertDeduplicator(Redis)로 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAlertMonitor {

    static final String DB_DOWN = "DB_DOWN";
    static final String REDIS_DOWN = "REDIS_DOWN";
    static final String ERROR_SURGE = "ERROR_SURGE";
    static final String STORAGE_80 = "STORAGE_80";
    static final String STORAGE_95 = "STORAGE_95";

    private final AdminMonitoringService monitoringService;
    private final ServerErrorRecorder serverErrorRecorder;
    private final AdminAlertService alertService;
    private final AlertDeduplicator deduplicator;

    @Value("${app.admin.alert.error-surge-threshold:10}")
    private int errorSurgeThreshold = 10;

    private final Set<String> active = new HashSet<>();

    @Scheduled(fixedDelayString = "${app.admin.alert.check-interval-ms:60000}",
            initialDelayString = "${app.admin.alert.check-interval-ms:60000}")
    public void check() {
        try {
            alertService.refreshTargets();
        } catch (RuntimeException e) {
            log.debug("운영자 알림 수신 기기 갱신 실패 (이전 목록 사용): {}", e.getMessage());
        }
        try {
            MonitoringResponse status = monitoringService.get();
            String instance = status.server().instance();

            Dependency db = status.dependencies().stream().filter(d -> d.name().equals("database")).findFirst().orElse(null);
            Dependency redis = status.dependencies().stream().filter(d -> d.name().equals("redis")).findFirst().orElse(null);
            long recentErrors = serverErrorRecorder.countSince(LocalDateTime.now().minusMinutes(5));

            outage(DB_DOWN, db != null && db.level() == Level.DOWN, instance);
            outage(REDIS_DOWN, redis != null && redis.level() == Level.DOWN, instance);
            outage(ERROR_SURGE, recentErrors >= errorSurgeThreshold, instance, recentErrors);

            if (status.storage() != null) { // DB 장애 중이면 저장소 값을 모르므로 판단 보류
                double usage = status.storage().usagePercent();
                threshold(STORAGE_95, usage >= 95.0, usage);
                threshold(STORAGE_80, usage >= 80.0, usage);
            }
        } catch (RuntimeException e) {
            // DB가 죽어 수신자 조회가 실패하는 경우 등 — 다음 주기에 다시 시도
            log.warn("운영자 알림 점검 실패: {}", e.getMessage());
        }
    }

    /** 장애: 시작·복구 각각 1번 */
    private void outage(String key, boolean failing, Object... args) {
        if (failing && active.add(key)) {
            if (deduplicator.claimActivation(key)) {
                alertService.send(Category.SYSTEM, "push.admin." + key, args);
            }
        } else if (!failing && active.remove(key)) {
            if (deduplicator.claimRecovery(key)) {
                alertService.send(Category.SYSTEM, "push.admin." + key + ".recovered", args);
            }
        }
    }

    /** 저장소: 넘을 때만 1번, 내려가면 조용히 초기화 */
    private void threshold(String key, boolean over, double usage) {
        if (over && active.add(key)) {
            if (deduplicator.claimActivation(key)) {
                alertService.send(Category.STORAGE, "push.admin." + key, usage);
            }
        } else if (!over && active.remove(key)) {
            deduplicator.claimRecovery(key);
        }
    }
}
