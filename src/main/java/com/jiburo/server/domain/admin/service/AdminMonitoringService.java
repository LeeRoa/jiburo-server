package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.Storage;
import com.jiburo.server.domain.admin.dto.AdminMonitoringDtos.*;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.repository.ReportRepository;
import com.jiburo.server.global.monitoring.ServerErrorRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 관리자 시스템 모니터링 — 문제가 생겼을 때 한 화면에서 원인 범위를 좁히는 용도.
 * 값은 요청을 받은 서버(인스턴스) 기준이며, 장기 추이는 별도 모니터링 도구(로그·APM)로 본다.
 */
@Service
@RequiredArgsConstructor
public class AdminMonitoringService {

    private static final double HEAP_WARN_PERCENT = 90.0;
    private static final long SLOW_DEPENDENCY_MS = 500;
    private static final long STALE_UPLOAD_HOURS = 1;

    private final DataSource dataSource;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactory;
    private final ObjectProvider<SimpUserRegistry> simpUserRegistry;
    private final Environment environment;
    private final ServerErrorRecorder serverErrorRecorder;
    private final AdminDashboardService adminDashboardService;
    private final ReportRepository reportRepository;
    private final ImageMetaRepository imageMetaRepository;

    public MonitoringResponse get() {
        LocalDateTime now = LocalDateTime.now();
        Jvm jvm = jvm();
        List<Dependency> dependencies = List.of(checkDatabase(), checkRedis());
        Storage storage = adminDashboardService.storage();
        Errors errors = new Errors(serverErrorRecorder.totalSinceStart(),
                serverErrorRecorder.countSince(now.minusHours(1)), serverErrorRecorder.recent());
        Activity activity = new Activity(
                simpUserRegistry.stream().mapToInt(SimpUserRegistry::getUserCount).findFirst().orElse(0),
                reportRepository.countByStatus(ReportStatus.PENDING),
                imageMetaRepository.countByStatusCodeAndCreatedAtBefore(ImageStatus.PENDING, now.minusHours(STALE_UPLOAD_HOURS)));

        return new MonitoringResponse(now, overall(jvm, dependencies, storage, errors), server(), jvm,
                dependencies, storage, activity, errors);
    }

    private Overall overall(Jvm jvm, List<Dependency> deps, Storage storage, Errors errors) {
        List<String> reasons = new ArrayList<>();
        Level level = Level.UP;
        for (Dependency d : deps) {
            if (d.level() == Level.DOWN) {
                level = Level.DOWN;
                reasons.add(d.name() + " 연결 실패: " + d.error());
            } else if (d.level() == Level.WARN) {
                reasons.add(d.name() + " 응답 지연 " + d.latencyMs() + "ms");
            }
        }
        if (storage.warning()) {
            reasons.add("저장소 사용률 " + storage.usagePercent() + "%");
        }
        if (jvm.heapUsagePercent() >= HEAP_WARN_PERCENT) {
            reasons.add("메모리(힙) 사용률 " + jvm.heapUsagePercent() + "%");
        }
        if (errors.lastHour() > 0) {
            reasons.add("최근 1시간 서버 오류 " + errors.lastHour() + "건");
        }
        if (level == Level.UP && !reasons.isEmpty()) {
            level = Level.WARN;
        }
        return new Overall(level, reasons);
    }

    private ServerInfo server() {
        var runtime = ManagementFactory.getRuntimeMXBean();
        String instance;
        try {
            instance = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            instance = "unknown";
        }
        return new ServerInfo(instance,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(runtime.getStartTime()), ZoneId.systemDefault()),
                runtime.getUptime() / 1000, System.getProperty("java.version"),
                Arrays.asList(environment.getActiveProfiles()));
    }

    private Jvm jvm() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long max = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
        double heapPercent = max == 0 ? 0 : Math.round(heap.getUsed() * 1000.0 / max) / 10.0;
        double processCpu = -1;
        double systemCpu = -1;
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            processCpu = percent(os.getProcessCpuLoad());
            systemCpu = percent(os.getCpuLoad());
        }
        return new Jvm(heap.getUsed(), max, heapPercent, ManagementFactory.getThreadMXBean().getThreadCount(),
                Runtime.getRuntime().availableProcessors(), processCpu, systemCpu);
    }

    private static double percent(double load) {
        return load < 0 ? -1 : Math.round(load * 1000.0) / 10.0;
    }

    private Dependency checkDatabase() {
        long start = System.nanoTime();
        try (Connection c = dataSource.getConnection()) {
            if (!c.isValid(2)) {
                return new Dependency("database", Level.DOWN, -1, "연결 검증 실패");
            }
            return timed("database", start);
        } catch (Exception e) {
            return new Dependency("database", Level.DOWN, -1, e.getClass().getSimpleName());
        }
    }

    private Dependency checkRedis() {
        RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
        if (factory == null) {
            return new Dependency("redis", Level.DOWN, -1, "설정 없음");
        }
        long start = System.nanoTime();
        try (RedisConnection c = factory.getConnection()) {
            c.ping();
            return timed("redis", start);
        } catch (Exception e) {
            return new Dependency("redis", Level.DOWN, -1, e.getClass().getSimpleName());
        }
    }

    private static Dependency timed(String name, long startNanos) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000;
        return new Dependency(name, ms >= SLOW_DEPENDENCY_MS ? Level.WARN : Level.UP, ms, null);
    }
}
