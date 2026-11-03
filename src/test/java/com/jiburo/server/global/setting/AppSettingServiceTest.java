package com.jiburo.server.global.setting;

import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AppSettingServiceTest {

    private AppSettingRepository repository;
    private AppSettingService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppSettingRepository.class);
        given(repository.findAll()).willReturn(List.of());
        service = new AppSettingService(repository);
        service.init();
    }

    @Test
    void 저장된_값이_없으면_기본값을_사용한다() {
        assertThat(service.getInt(SettingKey.POST_MAX_IMAGES)).isEqualTo(5);
        assertThat(service.getDouble(SettingKey.NEARBY_DEFAULT_RADIUS_KM)).isEqualTo(3.0);
        assertThat(service.isOverridden(SettingKey.POST_MAX_IMAGES)).isFalse();
    }

    @Test
    void 관리자가_바꾼_값은_즉시_반영된다() {
        given(repository.findById(SettingKey.POST_MAX_IMAGES)).willReturn(Optional.empty());

        service.update(SettingKey.POST_MAX_IMAGES, " 8 ", UUID.randomUUID());

        assertThat(service.getInt(SettingKey.POST_MAX_IMAGES)).isEqualTo(8);
        assertThat(service.isOverridden(SettingKey.POST_MAX_IMAGES)).isTrue();
        verify(repository).save(any(AppSetting.class));
    }

    @Test
    void 범위를_벗어나거나_타입이_틀린_값은_저장하지_않는다() {
        assertThatThrownBy(() -> service.update(SettingKey.POST_MAX_IMAGES, "100", UUID.randomUUID()))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_SETTING_VALUE));
        assertThatThrownBy(() -> service.update(SettingKey.POST_MAX_IMAGES, "abc", UUID.randomUUID()))
                .isInstanceOf(JiburoException.class);
        verify(repository, never()).save(any());
        assertThat(service.getInt(SettingKey.POST_MAX_IMAGES)).isEqualTo(5);
    }

    @Test
    void 기본값_복원() {
        given(repository.findById(SettingKey.CHAT_MAX_TEXT_LENGTH)).willReturn(Optional.empty());
        service.update(SettingKey.CHAT_MAX_TEXT_LENGTH, "500", UUID.randomUUID());

        service.reset(SettingKey.CHAT_MAX_TEXT_LENGTH);

        assertThat(service.getInt(SettingKey.CHAT_MAX_TEXT_LENGTH)).isEqualTo(1000);
        verify(repository).deleteById(SettingKey.CHAT_MAX_TEXT_LENGTH);
    }

    @Test
    void 새로고침시_다른_서버에서_바꾼_값을_읽고_DB에_잘못된_값은_무시한다() {
        given(repository.findAll()).willReturn(List.of(
                new AppSetting(SettingKey.SEARCH_HISTORY_MAX, "20", UUID.randomUUID()),
                new AppSetting(SettingKey.POST_MAX_IMAGES, "999", UUID.randomUUID()) // 범위 밖
        ));

        service.refresh();

        assertThat(service.getInt(SettingKey.SEARCH_HISTORY_MAX)).isEqualTo(20);
        assertThat(service.getInt(SettingKey.POST_MAX_IMAGES)).isEqualTo(5);
    }

    @Test
    void 새로고침_중_DB_오류가_나도_기존_값을_유지한다() {
        given(repository.findById(SettingKey.SEARCH_HISTORY_MAX)).willReturn(Optional.empty());
        service.update(SettingKey.SEARCH_HISTORY_MAX, "20", UUID.randomUUID());
        given(repository.findAll()).willThrow(new RuntimeException("DB down"));

        service.refresh();

        assertThat(service.getInt(SettingKey.SEARCH_HISTORY_MAX)).isEqualTo(20);
    }

    @Test
    void 없는_설정_키는_SETTING_NOT_FOUND() {
        assertThat(AppSettingService.parseKey("post_max_images")).isEqualTo(SettingKey.POST_MAX_IMAGES);
        assertThatThrownBy(() -> AppSettingService.parseKey("NOPE"))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.SETTING_NOT_FOUND));
    }

    @Test
    void 모든_설정의_기본값은_자기_타입과_범위를_만족한다() {
        // 새 설정 추가 시 기본값을 잘못 적는 실수를 막는 안전장치
        for (SettingKey key : SettingKey.values()) {
            assertThat(key.getType().normalize(key.getDefaultValue(), key.getMin(), key.getMax()))
                    .as(key.name())
                    .isNotNull();
        }
    }
}
