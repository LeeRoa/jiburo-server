package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.admin.domain.AdminPushPreference;
import com.jiburo.server.domain.admin.repository.AdminPushPreferenceRepository;
import com.jiburo.server.domain.push.service.PushSender.PushMessage;
import com.jiburo.server.domain.push.service.PushService;
import com.jiburo.server.domain.push.service.PushText;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 운영자 푸시 발송 + 수신 설정.
 * 수신 대상 = 관리자 역할 보유 + 시스템 모니터링(MONITORING) 노출 권한 + 해당 항목 수신 켜짐.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAlertService {

    public enum Category { SYSTEM, STORAGE }

    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminPushPreferenceRepository preferenceRepository;
    private final PushService pushService;
    private final PushText pushText;

    // DB 장애 중에도 보낼 수 있도록 마지막으로 확인한 수신 기기를 기억한다
    private volatile Map<Category, List<PushService.DeviceTarget>> cachedTargets = Map.of();

    /** 점검 주기마다 호출 (DB가 정상일 때 갱신, 실패하면 이전 목록 유지) */
    public void refreshTargets() {
        Map<Category, List<PushService.DeviceTarget>> next = new java.util.EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            next.put(c, pushService.targetsOf(recipients(c)));
        }
        cachedTargets = next;
    }

    public void send(Category category, String messageKey, Object... args) {
        List<PushService.DeviceTarget> targets;
        try {
            targets = pushService.targetsOf(recipients(category));
        } catch (RuntimeException e) {
            targets = cachedTargets.getOrDefault(category, List.of());
            log.warn("운영자 알림 수신자 조회 실패 — 마지막으로 확인한 기기 {}대로 발송: {}", targets.size(), e.getMessage());
        }
        log.info("운영자 알림 {} → 기기 {}대", messageKey, targets.size());
        Function<java.util.Locale, PushMessage> message = locale -> new PushMessage(
                pushText.get(messageKey + ".title", null, locale, args),
                pushText.get(messageKey + ".body", null, locale, args),
                Map.of("type", "ADMIN_ALERT", "alert", messageKey));
        pushService.sendToTargets(targets, message);
    }

    List<UUID> recipients(Category category) {
        List<UUID> admins = userRepository.findAllByRoleCode(RoleType.ADMIN).stream()
                .map(User::getId)
                .filter(id -> adminAuthorizationService.can(id, AdminMenu.MONITORING, AdminAccessLevel.READ))
                .toList();
        Map<UUID, AdminPushPreference> prefs = preferenceRepository.findAllById(admins).stream()
                .collect(Collectors.toMap(AdminPushPreference::getUserId, p -> p));
        return admins.stream().filter(id -> {
            AdminPushPreference p = prefs.get(id);
            if (p == null) return true; // 기본 켜짐
            return category == Category.SYSTEM ? p.isSystemAlert() : p.isStorageAlert();
        }).toList();
    }

    // ── 내 수신 설정 ──

    public record PreferenceResponse(boolean systemAlert, boolean storageAlert, long registeredDevices,
                                     boolean pushEnabled, boolean receivesAlerts) {}

    public PreferenceResponse preference(UUID adminId, boolean pushEnabled) {
        AdminPushPreference p = preferenceRepository.findById(adminId).orElseGet(() -> new AdminPushPreference(adminId));
        return new PreferenceResponse(p.isSystemAlert(), p.isStorageAlert(), pushService.deviceCount(adminId), pushEnabled,
                adminAuthorizationService.can(adminId, AdminMenu.MONITORING, AdminAccessLevel.READ));
    }

    @org.springframework.transaction.annotation.Transactional
    public void updatePreference(UUID adminId, Boolean systemAlert, Boolean storageAlert) {
        AdminPushPreference p = preferenceRepository.findById(adminId)
                .orElseGet(() -> preferenceRepository.save(new AdminPushPreference(adminId)));
        p.change(systemAlert, storageAlert);
    }

    /** 기기 등록 확인용: 내 기기로만 테스트 푸시 */
    public void sendTest(UUID adminId) {
        pushService.sendToUsers(List.of(adminId), locale -> new PushMessage(
                pushText.get("push.admin.TEST.title", null, locale),
                pushText.get("push.admin.TEST.body", null, locale),
                Map.of("type", "ADMIN_ALERT", "alert", "TEST")));
    }
}
