package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.AddResult;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.TestResult;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.WordResponse;
import com.jiburo.server.domain.moderation.domain.BannedWord;
import com.jiburo.server.domain.moderation.repository.BannedWordRepository;
import com.jiburo.server.domain.moderation.service.BannedWordFilter;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBannedWordService {

    private final BannedWordRepository repository;
    private final UserRepository userRepository;
    private final BannedWordFilter filter;

    public Page<WordResponse> search(String keyword, Pageable pageable) {
        String k = keyword == null ? "" : BannedWordFilter.normalize(keyword);
        Page<BannedWord> page = repository.findByWordContaining(k, pageable);
        Map<UUID, String> nicknames = nicknamesOf(page.getContent());
        return page.map(w -> WordResponse.of(w, nicknames));
    }

    /** 여러 개를 한 번에 추가. 이미 있는 단어(정규화 기준)는 건너뛴다. */
    @Transactional
    public AddResult add(List<String> rawWords, UUID adminId) {
        LinkedHashSet<String> words = new LinkedHashSet<>();
        for (String raw : rawWords) {
            words.add(normalizeOrThrow(raw));
        }

        Set<String> existing = new HashSet<>();
        repository.findByWordIn(words).forEach(b -> existing.add(b.getWord()));

        List<BannedWord> saved = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String w : words) {
            if (existing.contains(w)) {
                skipped.add(w);
            } else {
                saved.add(repository.save(new BannedWord(w, adminId)));
            }
        }
        filter.invalidate();
        Map<UUID, String> nicknames = nicknamesOf(saved);
        return new AddResult(saved.stream().map(w -> WordResponse.of(w, nicknames)).toList(), skipped);
    }

    /** 금지어 수정 (오타 정정 등). 이미 다른 항목으로 있는 단어로는 바꿀 수 없다. */
    @Transactional
    public WordResponse update(Long wordId, String rawWord, UUID adminId) {
        BannedWord word = findOrThrow(wordId);
        String normalized = normalizeOrThrow(rawWord);
        if (!normalized.equals(word.getWord())) {
            boolean taken = repository.findByWordIn(List.of(normalized)).stream()
                    .anyMatch(other -> !other.getId().equals(wordId));
            if (taken) {
                throw new JiburoException(ErrorCode.BANNED_WORD_DUPLICATED);
            }
            word.change(normalized, adminId);
            repository.flush(); // 응답에 갱신된 수정 시각을 담기 위해
            filter.invalidate();
        }
        return WordResponse.of(word, nicknamesOf(List.of(word)));
    }

    @Transactional
    public void delete(Long wordId) {
        repository.delete(findOrThrow(wordId));
        filter.invalidate();
    }

    /** 등록된 금지어로 문장을 가려 보는 미리보기 */
    public TestResult test(String text) {
        return new TestResult(filter.mask(text), filter.contains(text));
    }

    private BannedWord findOrThrow(Long wordId) {
        return repository.findById(wordId)
                .orElseThrow(() -> new JiburoException(ErrorCode.BANNED_WORD_NOT_FOUND));
    }

    private static String normalizeOrThrow(String raw) {
        String w = BannedWordFilter.normalize(raw);
        if (w.isEmpty()) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return w;
    }

    // 등록자·수정자 닉네임을 한 번에 조회 (N+1 방지)
    private Map<UUID, String> nicknamesOf(Collection<BannedWord> words) {
        Set<UUID> ids = words.stream()
                .flatMap(w -> Stream.of(w.getCreatedBy(), w.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }
}
