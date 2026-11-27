package com.jiburo.server.global.i18n;

import org.springframework.context.support.AbstractMessageSource;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * MessageCatalog(파일 기본값 + 관리자 DB 수정값) 기반 MessageSource.
 * MessageSource는 컨텍스트 초기화 초반에 만들어지므로 DB 의존성을 직접 갖지 않고,
 * DB 값은 I18nMessageCache가 기동 후 catalog에 채워 넣는다.
 */
public class CatalogMessageSource extends AbstractMessageSource {

    private final MessageCatalog catalog;

    public CatalogMessageSource(MessageCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        return catalog.find(code, locale).map(message -> createMessageFormat(message, locale)).orElse(null);
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return catalog.find(code, locale).orElse(null);
    }
}
