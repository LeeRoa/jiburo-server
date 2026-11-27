package com.jiburo.server.domain.moderation.service;

import com.jiburo.server.domain.moderation.domain.BannedWord;
import com.jiburo.server.domain.moderation.repository.BannedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 금지어 검사/가리기.
 *
 * - 대소문자 무시, 글자 사이에 끼운 문장부호·기호도 잡는다 (예: "바.보", "B-A-D").
 *   공백은 허용하지 않는다 — 허용하면 "개시 발표"처럼 단어 경계를 넘어 오탐이 난다.
 * - 일치한 부분 전체를 "***"로 바꾼다.
 * - 금지어 목록은 정규식 하나로 합쳐 메모리에 둔다. 관리자 변경 시 커밋 후 비우고,
 *   다른 서버는 주기적 비우기(기본 60초)로 반영된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BannedWordFilter {

    public static final String MASK = "***";
    // 글자 사이에 허용하는 끼워넣기 문자: 문장부호, 기호 (공백 제외)
    private static final String GAP = "[\\p{P}\\p{S}]*";

    private final BannedWordRepository repository;

    // Optional.empty() = 금지어 없음, null = 아직 안 읽음
    private volatile Optional<Pattern> pattern;

    /** 금지어를 *** 로 가린 문자열 (null이면 null) */
    public String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return pattern().map(p -> p.matcher(text).replaceAll(Matcher.quoteReplacement(MASK))).orElse(text);
    }

    /** 금지어 포함 여부 (닉네임처럼 가리면 안 되고 거부해야 하는 곳에서 사용) */
    public boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return pattern().map(p -> p.matcher(text).find()).orElse(false);
    }

    /** 저장 형태로 정규화: 소문자 + 공백 제거 */
    public static String normalize(String word) {
        return word == null ? "" : word.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    public void invalidate() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pattern = null;
                }
            });
        } else {
            pattern = null;
        }
    }

    @Scheduled(fixedDelayString = "${app.moderation.banned-word-cache-ttl-ms:60000}")
    public void evictPeriodically() {
        pattern = null;
    }

    private Optional<Pattern> pattern() {
        Optional<Pattern> cached = pattern;
        if (cached == null) {
            cached = build(repository.findAll().stream().map(BannedWord::getWord).toList());
            pattern = cached;
        }
        return cached;
    }

    static Optional<Pattern> build(List<String> words) {
        List<String> parts = words.stream()
                .map(BannedWordFilter::normalize)
                .filter(w -> !w.isEmpty())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed()) // 긴 단어 먼저 (부분 일치로 덜 가려지지 않게)
                .map(BannedWordFilter::toRegex)
                .toList();
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Pattern.compile(String.join("|", parts),
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }

    private static String toRegex(String word) {
        return word.codePoints()
                .mapToObj(cp -> Pattern.quote(new String(Character.toChars(cp))))
                .collect(Collectors.joining(GAP));
    }
}
