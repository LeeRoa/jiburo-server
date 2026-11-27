package com.jiburo.server.domain.push.service;

import com.jiburo.server.global.i18n.MessageCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 푸시 문구 조회 (i18n/push/messages*.properties + 관리자 다국어 수정값).
 * 키가 없으면 fallbackKey로, 그것도 없으면 키 문자열 대신 빈 문자열을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PushText {

    private final MessageCatalog catalog;
    private final MessageSource messageSource;

    public String get(String key, String fallbackKey, Locale locale, Object... args) {
        if (catalog.find(key, locale).isPresent()) {
            return messageSource.getMessage(key, args, locale);
        }
        if (fallbackKey != null && catalog.find(fallbackKey, locale).isPresent()) {
            return messageSource.getMessage(fallbackKey, args, locale);
        }
        return "";
    }
}
