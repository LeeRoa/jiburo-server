package com.jiburo.server.global.setting;

import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 운영 설정 조회/변경.
 *
 * - 조회는 메모리 캐시에서만 하므로 요청마다 DB를 치지 않는다.
 * - 관리자가 값을 바꾸면 이 서버의 캐시는 즉시 갱신되고, 다른 서버(스케일 아웃 시)는
 *   주기적 refresh(기본 60초)로 반영된다.
 * - DB에서 읽은 값이 (enum 범위 변경 등으로) 더 이상 유효하지 않으면 무시하고 기본값을 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppSettingService {

    private final AppSettingRepository appSettingRepository;

    // 관리자가 바꾼 값만 보관 (없으면 SettingKey.defaultValue)
    private final Map<SettingKey, String> overrides = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${app.settings.refresh-interval-ms:60000}",
            initialDelayString = "${app.settings.refresh-interval-ms:60000}")
    public void refresh() {
        try {
            Map<SettingKey, String> loaded = new EnumMap<>(SettingKey.class);
            for (AppSetting setting : appSettingRepository.findAll()) {
                SettingKey key = setting.getSettingKey();
                try {
                    loaded.put(key, key.getType().normalize(setting.getSettingValue(), key.getMin(), key.getMax()));
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 운영 설정값 무시 (기본값 사용): key={}, value={}", key, setting.getSettingValue());
                }
            }
            overrides.keySet().retainAll(loaded.keySet());
            overrides.putAll(loaded);
        } catch (RuntimeException e) {
            // DB 장애 시에도 마지막으로 읽은 값으로 계속 동작
            log.warn("운영 설정 새로고침 실패 (기존 값 유지): {}", e.getMessage());
        }
    }

    // ── 타입별 조회 ──

    public String getString(SettingKey key) {
        return overrides.getOrDefault(key, key.getDefaultValue());
    }

    public int getInt(SettingKey key) {
        return Integer.parseInt(getString(key));
    }

    public long getLong(SettingKey key) {
        return Long.parseLong(getString(key));
    }

    public double getDouble(SettingKey key) {
        return Double.parseDouble(getString(key));
    }

    public boolean getBoolean(SettingKey key) {
        return Boolean.parseBoolean(getString(key));
    }

    public List<String> getStringList(SettingKey key) {
        return SettingType.splitList(getString(key));
    }

    public boolean isOverridden(SettingKey key) {
        return overrides.containsKey(key);
    }

    // ── 관리자 변경 ──

    public List<AppSetting> findAllStored() {
        return appSettingRepository.findAll();
    }

    public Optional<AppSetting> findStored(SettingKey key) {
        return appSettingRepository.findById(key);
    }

    @Transactional
    public void update(SettingKey key, String rawValue, UUID adminId) {
        String value;
        try {
            value = key.getType().normalize(rawValue, key.getMin(), key.getMax());
        } catch (IllegalArgumentException e) {
            throw new JiburoException(ErrorCode.INVALID_SETTING_VALUE);
        }

        appSettingRepository.findById(key)
                .ifPresentOrElse(
                        setting -> setting.change(value, adminId),
                        () -> appSettingRepository.save(new AppSetting(key, value, adminId))
                );
        overrides.put(key, value);
        log.info("운영 설정 변경: key={}, value={}, by={}", key, value, adminId);
    }

    // 기본값으로 되돌리기 (저장된 값 삭제)
    @Transactional
    public void reset(SettingKey key) {
        appSettingRepository.deleteById(key);
        overrides.remove(key);
        log.info("운영 설정 기본값 복원: key={}", key);
    }

    /**
     * 경로 변수 등 외부 입력을 SettingKey로 변환 (대소문자 무시). 없는 키면 SETTING_NOT_FOUND.
     */
    public static SettingKey parseKey(String raw) {
        return Arrays.stream(SettingKey.values())
                .filter(k -> k.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new JiburoException(ErrorCode.SETTING_NOT_FOUND));
    }
}
