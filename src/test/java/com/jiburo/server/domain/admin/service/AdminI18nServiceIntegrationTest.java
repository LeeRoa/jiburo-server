package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminI18nDtos.LocaleValue;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.MessageRow;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.SearchCondition;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.Status;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.i18n.I18nMessageCache;
import com.jiburo.server.global.i18n.I18nMessageRepository;
import com.jiburo.server.global.i18n.LabelResolver;
import org.springframework.context.i18n.LocaleContextHolder;
import com.jiburo.server.global.i18n.MessageCatalog.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MessageCatalog는 메모리 싱글톤이라 롤백 트랜잭션을 쓰면 캐시에 지운 값이 남는다.
 * 그래서 트랜잭션 없이 실행하고 매 테스트 후 DB를 비운 뒤 캐시를 다시 읽는다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminI18nServiceIntegrationTest {

    private static final String KEY = "error.emoticon.not_found"; // ko/en 파일 기본값이 있는 키

    @Autowired private AdminI18nService adminI18nService;
    @Autowired private I18nMessageRepository repository;
    @Autowired private I18nMessageCache cache;
    @Autowired private MessageSource messageSource;
    @Autowired private LabelResolver labelResolver;

    private final UUID adminId = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        cache.reload();
    }

    private LocaleValue value(MessageRow row, String locale) {
        return row.values().stream().filter(v -> v.locale().equals(locale)).findFirst().orElseThrow();
    }

    @Test
    void 언어_목록은_ko가_먼저_나온다() {
        assertThat(adminI18nService.locales()).startsWith("ko").contains("en");
    }

    @Test
    void 수정하면_즉시_MessageSource에_반영되고_되돌리면_파일_기본값으로_돌아간다() {
        String fileDefault = messageSource.getMessage(KEY, null, Locale.KOREAN);

        MessageRow updated = adminI18nService.update(KEY, "ko", "바뀐 문구", adminId);

        assertThat(value(updated, "ko").source()).isEqualTo(Source.DB);
        assertThat(value(updated, "ko").value()).isEqualTo("바뀐 문구");
        assertThat(value(updated, "ko").defaultValue()).isEqualTo(fileDefault);
        assertThat(value(updated, "ko").updatedBy()).isEqualTo(adminId);
        assertThat(value(updated, "en").source()).isEqualTo(Source.FILE);
        assertThat(messageSource.getMessage(KEY, null, Locale.KOREAN)).isEqualTo("바뀐 문구");

        MessageRow reset = adminI18nService.reset(KEY, "ko");

        assertThat(value(reset, "ko").source()).isEqualTo(Source.FILE);
        assertThat(messageSource.getMessage(KEY, null, Locale.KOREAN)).isEqualTo(fileDefault);
    }

    @Test
    void 새_키와_새_언어를_추가할_수_있고_언어_태그는_정규화된다() {
        MessageRow row = adminI18nService.update("custom.hello", "ja_jp", "こんにちは {0}", adminId);

        assertThat(adminI18nService.locales()).contains("ja-JP");
        assertThat(value(row, "ja-JP").value()).isEqualTo("こんにちは {0}");
        assertThat(value(row, "ko").source()).isEqualTo(Source.NONE);
        assertThat(messageSource.getMessage("custom.hello", new Object[]{"지부로"}, Locale.JAPAN)).isEqualTo("こんにちは 지부로");

        // 추가했던 키의 마지막 문구를 지우면 키가 사라진다
        adminI18nService.reset("custom.hello", "ja-JP");
        assertThatThrownBy(() -> adminI18nService.find("custom.hello"))
                .extracting("errorCode").isEqualTo(ErrorCode.I18N_MESSAGE_NOT_FOUND);
    }

    @Test
    void 잘못된_키_언어_형식은_저장하지_않는다() {
        assertThatThrownBy(() -> adminI18nService.update("잘못된 키", "ko", "문구", adminId))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_I18N_KEY);
        assertThatThrownBy(() -> adminI18nService.update(KEY, "한국어", "문구", adminId))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_I18N_LOCALE);
        assertThatThrownBy(() -> adminI18nService.update(KEY, "ko", "깨진 {0 문구", adminId))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_I18N_MESSAGE_FORMAT);
        assertThat(repository.count()).isZero();
    }

    @Test
    void 문구가_없는_공통코드_키도_누락으로_나오고_채우면_라벨로_내려간다() {
        String key = "chat.msg.emoticon"; // ChatMsgType.EMOTICON의 message_key (properties에 없음)
        LocaleContextHolder.setLocale(Locale.KOREAN);
        try {
            assertThat(labelResolver.resolve(key)).isNull();
            assertThat(adminI18nService.search(new SearchCondition(null, "chat.msg.", Status.MISSING), PageRequest.of(0, 50))
                    .getContent()).extracting(MessageRow::key).contains(key);

            adminI18nService.update(key, "ko", "이모티콘", adminId);

            assertThat(labelResolver.resolve(key)).isEqualTo("이모티콘");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void 검색은_키워드_접두사_누락_수정_필터를_지원한다() {
        adminI18nService.update(KEY, "ko", "이모티콘 없음 (수정됨)", adminId);
        adminI18nService.update("custom.only.ko", "ko", "한국어만 있는 문구", adminId);

        assertThat(adminI18nService.search(new SearchCondition("수정됨", null, null), PageRequest.of(0, 50))
                .getContent()).extracting(MessageRow::key).containsExactly(KEY);
        assertThat(adminI18nService.search(new SearchCondition(null, "custom.", Status.MISSING), PageRequest.of(0, 50))
                .getContent()).extracting(MessageRow::key).containsExactly("custom.only.ko");
        assertThat(adminI18nService.search(new SearchCondition(null, null, Status.OVERRIDDEN), PageRequest.of(0, 50))
                .getContent()).extracting(MessageRow::key).containsExactlyInAnyOrder(KEY, "custom.only.ko");

        var page = adminI18nService.search(new SearchCondition(null, "error.", null), PageRequest.of(1, 5));
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isGreaterThan(10);
    }
}
