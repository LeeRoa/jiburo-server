package com.jiburo.server.global.config;

import com.jiburo.server.global.i18n.CatalogMessageSource;
import com.jiburo.server.global.i18n.MessageCatalog;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 다국어 MessageSource: i18n/** properties(파일 기본값) + 관리자 DB 수정값(i18n_messages).
 * 조회 순서와 대체 규칙은 MessageCatalog 참고.
 */
@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageCatalog messageCatalog() {
        return MessageCatalog.loadFromClasspath("classpath*:i18n/**/messages*.properties");
    }

    @Bean
    public MessageSource messageSource(MessageCatalog messageCatalog) {
        CatalogMessageSource messageSource = new CatalogMessageSource(messageCatalog);
        messageSource.setAlwaysUseMessageFormat(true); // 메시지 포맷팅 활성화
        messageSource.setUseCodeAsDefaultMessage(true); // 메시지 못 찾으면 코드 그대로 출력
        return messageSource;
    }
}
