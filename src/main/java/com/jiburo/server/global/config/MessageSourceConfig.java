package com.jiburo.server.global.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        messageSource.setBasenames(
                "classpath:i18n/common/messages",
                "classpath:i18n/error/messages",
                "classpath:i18n/validation/messages"
        );

        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setAlwaysUseMessageFormat(true); // 메시지 포맷팅 활성화
        messageSource.setUseCodeAsDefaultMessage(true); // 메시지 못 찾으면 코드 그대로 출력
        messageSource.setFallbackToSystemLocale(false);

        return messageSource;
    }
}