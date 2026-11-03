package com.jiburo.server.global.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 서버 다국어 문구 저장소 (메모리).
 *
 * - 파일 기본값: classpath의 i18n/**\/messages[_locale].properties (접미사 없는 파일 = 기준 언어 ko)
 * - 관리자 수정값: DB(i18n_messages) — I18nMessageCache가 주기적으로 채운다
 *
 * 조회 순서 (요청 언어 L):
 *   1) DB[L]  2) 파일[L]  3) DB[ko]  4) 파일[ko]  → 없으면 empty
 * L은 정확한 태그(zh-CN) 다음 언어 코드(zh) 순으로 찾는다.
 * 관리자가 고친 ko 문구가 "번역이 없는 언어"에서도 파일 기본값보다 우선하도록 3)을 2)의 ko 대체보다 앞에 둔다.
 */
@Slf4j
public class MessageCatalog {

    public static final String BASE_LOCALE = "ko";

    private static final Pattern FILE_LOCALE = Pattern.compile("messages(?:_([A-Za-z]{2,3}(?:_[A-Za-z0-9]{2,4})?))?\\.properties$");

    public enum Source { DB, FILE, NONE }

    // locale -> (key -> message)
    private final Map<String, Map<String, String>> fileMessages;
    private volatile Map<String, Map<String, String>> dbMessages = Map.of();

    public MessageCatalog(Map<String, Map<String, String>> fileMessages) {
        Map<String, Map<String, String>> copy = new HashMap<>();
        fileMessages.forEach((locale, messages) -> copy.put(locale, Map.copyOf(messages)));
        this.fileMessages = Map.copyOf(copy);
    }

    public static MessageCatalog loadFromClasspath(String locationPattern) {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver().getResources(locationPattern)) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                Matcher m = FILE_LOCALE.matcher(filename);
                if (!m.find()) continue;
                String locale = m.group(1) == null ? BASE_LOCALE : normalizeTag(m.group(1));
                Properties props = new Properties();
                try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                Map<String, String> bucket = result.computeIfAbsent(locale, k -> new HashMap<>());
                props.forEach((k, v) -> bucket.put(k.toString(), v.toString()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("i18n properties 로드 실패: " + locationPattern, e);
        }
        log.info("MessageCatalog 파일 문구 로드: {}", result.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().size()).toList());
        return new MessageCatalog(result);
    }

    /** "zh_CN" / "zh-cn" → "zh-CN", "EN" → "en" */
    public static String normalizeTag(String tag) {
        return Locale.forLanguageTag(tag.trim().replace('_', '-')).toLanguageTag();
    }

    public void replaceDbMessages(Map<String, Map<String, String>> messages) {
        Map<String, Map<String, String>> copy = new HashMap<>();
        messages.forEach((locale, m) -> copy.put(locale, Map.copyOf(m)));
        this.dbMessages = Map.copyOf(copy);
    }

    // ── 조회 ──

    public Optional<String> find(String key, Locale locale) {
        List<String> candidates = candidates(locale);
        for (String tag : candidates) {
            String v = get(dbMessages, tag, key);
            if (v != null) return Optional.of(v);
            v = get(fileMessages, tag, key);
            if (v != null) return Optional.of(v);
        }
        String v = get(dbMessages, BASE_LOCALE, key);
        if (v != null) return Optional.of(v);
        return Optional.ofNullable(get(fileMessages, BASE_LOCALE, key));
    }

    /** 대체 없이 정확히 그 언어에 등록된 값 (관리자 화면용) */
    public Optional<String> findExact(String key, String localeTag) {
        String v = get(dbMessages, localeTag, key);
        return Optional.ofNullable(v != null ? v : get(fileMessages, localeTag, key));
    }

    public Source sourceOf(String key, String localeTag) {
        if (get(dbMessages, localeTag, key) != null) return Source.DB;
        if (get(fileMessages, localeTag, key) != null) return Source.FILE;
        return Source.NONE;
    }

    public Optional<String> fileValue(String key, String localeTag) {
        return Optional.ofNullable(get(fileMessages, localeTag, key));
    }

    /** 파일 + DB에 있는 모든 키 */
    public Set<String> allKeys() {
        Set<String> keys = new TreeSet<>();
        fileMessages.values().forEach(m -> keys.addAll(m.keySet()));
        dbMessages.values().forEach(m -> keys.addAll(m.keySet()));
        return keys;
    }

    private static String get(Map<String, Map<String, String>> source, String locale, String key) {
        Map<String, String> m = source.get(locale);
        return m == null ? null : m.get(key);
    }

    private static List<String> candidates(Locale locale) {
        if (locale == null) return List.of();
        List<String> tags = new ArrayList<>(2);
        String full = locale.toLanguageTag();
        if (!full.isEmpty() && !"und".equals(full)) tags.add(full);
        String language = locale.getLanguage();
        if (!language.isEmpty() && !tags.contains(language)) tags.add(language);
        return tags;
    }
}
