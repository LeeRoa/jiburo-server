package com.jiburo.server.global.i18n;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * DB(i18n_messages) 문구를 MessageCatalog에 반영한다.
 * 관리자 수정 시 즉시 reload, 다른 서버(스케일 아웃)는 주기적 새로고침(기본 60초)으로 반영.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class I18nMessageCache {

    private final I18nMessageRepository repository;
    private final MessageCatalog catalog;

    @PostConstruct
    public void init() {
        reload();
    }

    @Scheduled(fixedDelayString = "${app.i18n.refresh-interval-ms:60000}",
            initialDelayString = "${app.i18n.refresh-interval-ms:60000}")
    public void reload() {
        try {
            Map<String, Map<String, String>> byLocale = new HashMap<>();
            for (I18nMessage m : repository.findAll()) {
                byLocale.computeIfAbsent(m.getLocale(), k -> new HashMap<>()).put(m.getMessageKey(), m.getMessage());
            }
            catalog.replaceDbMessages(byLocale);
        } catch (RuntimeException e) {
            log.warn("다국어 문구 새로고침 실패 (기존 값 유지): {}", e.getMessage());
        }
    }
}
