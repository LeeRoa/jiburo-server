package com.jiburo.server.global.util;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {

    private static MessageSource messageSource;

    // 1. 생성자 주입으로 변경 (Lombok 제거)
    // @Qualifier("messageSource")를 통해 내가 만든 빈을 주입받음
    public MessageUtils(@Qualifier("messageSource") MessageSource source) {
        MessageUtils.messageSource = source;
    }

    /**
     * 메시지 코드로 다국어 메시지 조회 (매개변수 없음)
     */
    public static String getMessage(String code) {
        return getMessage(code, null);
    }

    /**
     * 메시지 코드로 다국어 메시지 조회 (매개변수 포함)
     */
    public static String getMessage(String code, Object[] args) {
        if (messageSource == null) {
            return code; // 빈 초기화 전에 호출될 경우 방어 코드
        }

        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            // 메시지를 못 찾거나 에러 발생 시 코드값 그대로 반환
            // 로그를 남겨두면 디버깅에 좋습니다.
            // log.error("Message lookup failed for code: {}", code);
            return code;
        }
    }
}