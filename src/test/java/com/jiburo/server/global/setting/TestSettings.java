package com.jiburo.server.global.setting;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 단위 테스트용 AppSettingService — DB 없이 SettingKey 기본값으로 동작한다.
 * 서비스 테스트에서 {@code @Spy AppSettingService appSettingService = TestSettings.defaults();} 로 주입하고,
 * 특정 값이 필요하면 {@code TestSettings.with(appSettingService, SettingKey.X, "값")} 으로 바꾼다.
 */
public final class TestSettings {

    private TestSettings() {
    }

    public static AppSettingService defaults() {
        AppSettingRepository repository = mock(AppSettingRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        AppSettingService service = new AppSettingService(repository);
        service.init();
        return service;
    }

    public static void with(AppSettingService service, SettingKey key, String value) {
        service.update(key, value, null);
    }
}
