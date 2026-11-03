package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.dto.AdminAuditLogDtos;
import com.jiburo.server.domain.admin.dto.AdminDashboardDtos.*;
import com.jiburo.server.domain.admin.repository.AdminDashboardQueryRepository;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.global.setting.AppSettingService;
import com.jiburo.server.global.setting.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final long GB = 1024L * 1024 * 1024;
    private static final double STORAGE_WARNING_PERCENT = 80.0;
    private static final int RECENT_ADMIN_ACTIONS = 5;

    private final AdminDashboardQueryRepository dashboardQueryRepository;
    private final ImageMetaRepository imageMetaRepository;
    private final AppSettingService appSettingService;
    private final AdminAuditLogService adminAuditLogService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final Clock clock = Clock.systemDefaultZone();

    public DashboardResponse get(UUID viewerId, Period period) {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.minusDays(period.days() - 1L);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        Summary summary = new Summary(
                dashboardQueryRepository.countNewUsers(from, to),
                dashboardQueryRepository.countNewPosts(from, to),
                dashboardQueryRepository.countCompletedPosts(from, to),
                dashboardQueryRepository.countNewChatRooms(from, to)
        );

        Map<LocalDate, Long> users = dashboardQueryRepository.dailyNewUsers(from, to);
        Map<LocalDate, Long> posts = dashboardQueryRepository.dailyNewPosts(from, to);
        List<DailyPoint> daily = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(today); d = d.plusDays(1)) {
            daily.add(new DailyPoint(d, users.getOrDefault(d, 0L), posts.getOrDefault(d, 0L)));
        }

        // 최근 관리자 작업은 감사 로그를 볼 수 있는 관리자에게만 노출
        List<AdminAuditLogDtos.LogResponse> recent = null;
        if (adminAuthorizationService.can(viewerId, AdminMenu.AUDIT_LOG, AdminAccessLevel.READ)) {
            recent = adminAuditLogService.search(
                    new AdminAuditLogDtos.SearchCondition(null, null, "ADMIN_", null, null),
                    PageRequest.of(0, RECENT_ADMIN_ACTIONS)).getContent();
        }

        return new DashboardResponse(period, fromDate, today, summary, storage(), daily, recent);
    }

    private Storage storage() {
        long used = imageMetaRepository.sumTotalFileSize();
        long capacity = appSettingService.getInt(SettingKey.UPLOAD_TOTAL_CAPACITY_GB) * GB;
        double percent = capacity == 0 ? 0 : Math.round(used * 1000.0 / capacity) / 10.0;
        return new Storage(used, capacity, percent, percent >= STORAGE_WARNING_PERCENT);
    }
}
