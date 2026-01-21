package com.jiburo.server.global.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MessageUtils {

    private final MessageSource source;
    private static MessageSource messageSource;

    // 빈이 생성될 때 static 변수에 주입 (Static 메서드에서 사용하기 위함)
    @PostConstruct
    public void init() {
        messageSource = this.source;
    }

    /**
     * 메시지 코드로 다국어 메시지 조회 (매개변수 없음)
     */
    public static String getMessage(String code) {
        return getMessage(code, null);
    }

    /**
     * 메시지 코드로 다국어 메시지 조회 (매개변수 포함)
     * 예: "User {0} not found" -> getMessage("error.user", new Object[]{"Hong"})
     */
    public static String getMessage(String code, Object[] args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            // 메시지를 못 찾거나 에러 발생 시 코드값 그대로 반환
            return code;
        }
    }
}