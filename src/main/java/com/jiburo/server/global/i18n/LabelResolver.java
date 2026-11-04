package com.jiburo.server.global.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 화면 표시용 라벨 조회 (요청 언어 기준, MessageCatalog 대체 규칙 적용).
 * MessageUtils와 달리 문구가 없으면 키 대신 null을 돌려준다 — 프론트가 자체 번역으로 대체할 수 있도록.
 */
@Component
@RequiredArgsConstructor
public class LabelResolver {

    private final MessageCatalog catalog;
    private final MessageSource messageSource;

    public String resolve(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        var locale = LocaleContextHolder.getLocale();
        // MessageSource는 '코드를 기본 메시지로' 설정이라 없는 키도 키를 돌려주므로 존재 여부는 catalog로 판단
        if (catalog.find(key, locale).isEmpty()) {
            return null;
        }
        return messageSource.getMessage(key, null, locale); // '' 등 MessageFormat 처리를 일반 메시지와 동일하게
    }
}
