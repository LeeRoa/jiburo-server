package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminSettingResponseDto;
import com.jiburo.server.global.setting.AppSetting;
import com.jiburo.server.global.setting.AppSettingService;
import com.jiburo.server.global.setting.SettingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSettingService {

    private final AppSettingService appSettingService;

    // enum에 정의된 모든 설정을 (기본값 포함) 분류 순서대로 반환
    public List<AdminSettingResponseDto> findAll() {
        Map<SettingKey, AppSetting> stored = appSettingService.findAllStored().stream()
                .collect(Collectors.toMap(AppSetting::getSettingKey, Function.identity()));

        return Arrays.stream(SettingKey.values())
                .map(key -> AdminSettingResponseDto.of(key, appSettingService.getString(key), stored.get(key)))
                .toList();
    }

    public AdminSettingResponseDto find(String rawKey) {
        SettingKey key = AppSettingService.parseKey(rawKey);
        return toDto(key);
    }

    public AdminSettingResponseDto update(String rawKey, String value, UUID adminId) {
        SettingKey key = AppSettingService.parseKey(rawKey);
        appSettingService.update(key, value, adminId);
        return toDto(key);
    }

    public AdminSettingResponseDto reset(String rawKey) {
        SettingKey key = AppSettingService.parseKey(rawKey);
        appSettingService.reset(key);
        return toDto(key);
    }

    private AdminSettingResponseDto toDto(SettingKey key) {
        return AdminSettingResponseDto.of(key, appSettingService.getString(key),
                appSettingService.findStored(key).orElse(null));
    }
}
