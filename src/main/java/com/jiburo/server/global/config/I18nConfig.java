package com.jiburo.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        // HTTP 요청 헤더의 'Accept-Language'를 분석하여 언어를 결정
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

        // 언어 정보가 없을 경우 기본값을 한국어로 설정
        resolver.setDefaultLocale(Locale.KOREAN);
        return resolver;
    }
}