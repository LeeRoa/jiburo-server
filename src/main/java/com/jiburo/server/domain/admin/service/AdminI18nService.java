package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminI18nDtos.*;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.i18n.I18nMessage;
import com.jiburo.server.global.i18n.I18nMessageCache;
import com.jiburo.server.global.i18n.I18nMessageRepository;
import com.jiburo.server.global.i18n.MessageCatalog;
import com.jiburo.server.global.i18n.MessageCatalog.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 관리자 다국어 문구 관리.
 * - 목록의 키 = properties 파일 키 + 관리자가 추가한 키. 언어 = 파일/DB에 문구가 있는 언어 (ko 먼저).
 * - 수정은 DB(i18n_messages)에 저장되어 properties 기본값보다 우선한다. 되돌리기 = DB 값 삭제.
 * - 저장 직후 이 서버의 캐시를 다시 읽고, 다른 서버는 I18nMessageCache 주기(기본 60초)로 반영된다.
 *   그래서 트랜잭션을 걸지 않는다 (저장이 커밋된 뒤 reload해야 새 값이 보임).
 */
@Service
@RequiredArgsConstructor
public class AdminI18nService {

    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9_.-]{1,200}$");
    private static final Pattern LOCALE_TAG = Pattern.compile("^[a-z]{2,3}(-[A-Za-z0-9]{2,8})*$");

    private final MessageCatalog catalog;
    private final I18nMessageRepository repository;
    private final I18nMessageCache cache;

    public List<String> locales() {
        return catalog.locales();
    }

    public Page<MessageRow> search(SearchCondition c, Pageable pageable) {
        List<String> locales = catalog.locales();
        Map<String, Map<String, I18nMessage>> stored = storedByKey(repository.findAll());
        String keyword = c.keyword() == null || c.keyword().isBlank() ? null : c.keyword().trim().toLowerCase(Locale.ROOT);
        Status status = c.status() == null ? Status.ALL : c.status();

        List<MessageRow> rows = catalog.allKeys().stream()
                .filter(key -> c.prefix() == null || c.prefix().isBlank() || key.startsWith(c.prefix().trim()))
                .map(key -> toRow(key, locales, stored.getOrDefault(key, Map.of())))
                .filter(row -> keyword == null || matches(row, keyword))
                .filter(row -> switch (status) {
                    case ALL -> true;
                    case MISSING -> row.values().stream().anyMatch(v -> v.source() == Source.NONE);
                    case OVERRIDDEN -> row.values().stream().anyMatch(v -> v.source() == Source.DB);
                })
                .toList();

        int from = (int) Math.min(pageable.getOffset(), rows.size());
        int to = Math.min(from + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(from, to), pageable, rows.size());
    }

    public MessageRow find(String key) {
        Map<String, I18nMessage> stored = storedOf(key);
        if (stored.isEmpty() && !catalog.allKeys().contains(key)) {
            throw new JiburoException(ErrorCode.I18N_MESSAGE_NOT_FOUND);
        }
        return toRow(key, localesWith(stored.keySet()), stored);
    }

    /** 수정 또는 새 키/새 언어 추가 */
    public MessageRow update(String key, String rawLocale, String message, UUID adminId) {
        validateKey(key);
        String locale = normalizeLocale(rawLocale);
        validateFormat(message);

        I18nMessage entity = repository.findByMessageKeyAndLocale(key, locale)
                .orElseGet(() -> new I18nMessage(key, locale, message, adminId));
        entity.change(message, adminId);
        repository.save(entity);

        cache.reload();
        return find(key);
    }

    /** 관리자 수정값을 지우고 properties 기본값으로 되돌림 (기본값이 없는 키면 그 언어 문구가 사라짐) */
    public MessageRow reset(String key, String rawLocale) {
        String locale = normalizeLocale(rawLocale);
        Optional<I18nMessage> stored = repository.findByMessageKeyAndLocale(key, locale);
        if (stored.isEmpty() && !catalog.allKeys().contains(key)) {
            throw new JiburoException(ErrorCode.I18N_MESSAGE_NOT_FOUND);
        }
        stored.ifPresent(repository::delete);
        cache.reload();
        // 관리자가 추가했던 키의 마지막 문구를 지우면 키 자체가 사라지므로 존재 확인 없이 현재 상태를 돌려준다
        Map<String, I18nMessage> remaining = storedOf(key);
        return toRow(key, localesWith(remaining.keySet()), remaining);
    }

    // ── 내부 ──

    private MessageRow toRow(String key, List<String> locales, Map<String, I18nMessage> stored) {
        List<LocaleValue> values = locales.stream().map(locale -> {
            I18nMessage db = stored.get(locale);
            String fileValue = catalog.fileValue(key, locale).orElse(null);
            if (db != null) {
                return new LocaleValue(locale, db.getMessage(), Source.DB, fileValue, db.getUpdatedAt(), db.getUpdatedBy());
            }
            return new LocaleValue(locale, fileValue, fileValue != null ? Source.FILE : Source.NONE, fileValue, null, null);
        }).toList();
        return new MessageRow(key, values);
    }

    private static boolean matches(MessageRow row, String keyword) {
        return row.key().toLowerCase(Locale.ROOT).contains(keyword)
                || row.values().stream().map(LocaleValue::value)
                .anyMatch(v -> v != null && v.toLowerCase(Locale.ROOT).contains(keyword));
    }

    private Map<String, I18nMessage> storedOf(String key) {
        return storedByKey(repository.findAllByMessageKey(key)).getOrDefault(key, Map.of());
    }

    private static Map<String, Map<String, I18nMessage>> storedByKey(List<I18nMessage> messages) {
        Map<String, Map<String, I18nMessage>> result = new HashMap<>();
        for (I18nMessage m : messages) {
            result.computeIfAbsent(m.getMessageKey(), k -> new HashMap<>()).put(m.getLocale(), m);
        }
        return result;
    }

    // 다른 서버가 방금 추가한 언어가 아직 캐시에 없어도 상세에는 보이도록 DB 언어를 합친다
    private List<String> localesWith(Collection<String> extra) {
        List<String> locales = new ArrayList<>(catalog.locales());
        extra.stream().filter(l -> !locales.contains(l)).sorted().forEach(locales::add);
        return locales;
    }

    private static void validateKey(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new JiburoException(ErrorCode.INVALID_I18N_KEY);
        }
    }

    private static String normalizeLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new JiburoException(ErrorCode.INVALID_I18N_LOCALE);
        }
        String tag = MessageCatalog.normalizeTag(raw);
        // 해석할 수 없는 값(예: "한국어")은 "und"(미정)가 되므로 따로 거른다
        if ("und".equals(tag) || tag.length() > 20 || !LOCALE_TAG.matcher(tag).matches()) {
            throw new JiburoException(ErrorCode.INVALID_I18N_LOCALE);
        }
        return tag;
    }

    // MessageSource가 항상 MessageFormat으로 처리하므로, 깨진 형식이 저장되면 그 문구를 쓰는 응답이 실패한다
    private static void validateFormat(String message) {
        try {
            new MessageFormat(message);
        } catch (IllegalArgumentException e) {
            throw new JiburoException(ErrorCode.INVALID_I18N_MESSAGE_FORMAT);
        }
    }
}
