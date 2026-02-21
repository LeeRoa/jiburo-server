package com.jiburo.server.global.cache;

import com.jiburo.server.global.domain.CommonCode;
import com.jiburo.server.global.repository.CommonCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 앱 시작 시 common_codes 테이블을 메모리에 로드하여
 * DB JOIN 없이 O(1)로 코드 정보를 조회할 수 있게 합니다.
 *
 * 구조: Map<codeGroup, Map<code, CommonCode>>
 * 예: cache.get("STATUS").get("LOST") → CommonCode 엔티티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonCodeCache {

    private final CommonCodeRepository commonCodeRepository;

    private final Map<String, Map<String, CommonCode>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 캐시 전체 리로드 (초기화 또는 관리자 갱신 시 호출)
     */
    public void reload() {
        Map<String, Map<String, CommonCode>> newCache = commonCodeRepository.findAllByUseYnTrue()
                .stream()
                .collect(Collectors.groupingBy(
                        CommonCode::getCodeGroup,
                        Collectors.toMap(CommonCode::getCode, code -> code)
                ));

        cache.clear();
        cache.putAll(newCache);

        log.info("CommonCodeCache loaded: {} groups, {} total codes",
                cache.size(),
                cache.values().stream().mapToInt(Map::size).sum());
    }

    /**
     * 단건 조회 — codeGroup + code로 CommonCode 반환
     * @return Optional.empty() if not found
     */
    public Optional<CommonCode> get(String codeGroup, String code) {
        return Optional.ofNullable(cache.getOrDefault(codeGroup, Map.of()).get(code));
    }

    /**
     * 그룹 전체 조회 — 해당 그룹의 모든 코드 리스트 반환 (드롭다운, 필터 등)
     */
    public List<CommonCode> getGroup(String codeGroup) {
        Map<String, CommonCode> group = cache.get(codeGroup);
        if (group == null) {
            return List.of();
        }
        return List.copyOf(group.values());
    }

    /**
     * 프론트 전달용 — 활성화된 전체 코드를 그룹별로 반환
     */
    public Map<String, List<CommonCode>> getAll() {
        return cache.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue().values())
                ));
    }
}
