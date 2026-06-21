package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.dto.LocationHistoryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private static final int MAX_HISTORY = 10;
    private static final String KEYWORD_PREFIX = "search:keyword:";
    private static final String LOCATION_PREFIX = "search:location:";
    private static final String LOCATION_PLACE_PREFIX = "search:location-place:";
    private static final String LOCATION_ADDRESS_PREFIX = "search:location-address:";

    private final RedisTemplate<String, Object> redisTemplate;

    // ───────────────────────────────────────────────────
    // 키워드 검색 기록
    // ───────────────────────────────────────────────────

    @Override
    public void saveKeyword(UUID userId, String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        try {
            String key = KEYWORD_PREFIX + userId;
            redisTemplate.opsForZSet().add(key, keyword, System.currentTimeMillis());
            trimZSet(key);
        } catch (Exception e) {
            log.warn("키워드 검색 기록 저장 실패: userId={}", userId, e);
        }
    }

    @Override
    public List<String> getKeywords(UUID userId) {
        Set<Object> result = redisTemplate.opsForZSet().reverseRange(KEYWORD_PREFIX + userId, 0, -1);
        if (result == null) return List.of();
        return result.stream().map(Object::toString).toList();
    }

    @Override
    public void deleteKeyword(UUID userId, String keyword) {
        redisTemplate.opsForZSet().remove(KEYWORD_PREFIX + userId, keyword);
    }

    @Override
    public void deleteAllKeywords(UUID userId) {
        redisTemplate.delete(KEYWORD_PREFIX + userId);
    }

    // ───────────────────────────────────────────────────
    // 위치 검색 기록
    // ZSet:   member = 좌표 키 (dedup 기준)
    // Hash 1: field = 좌표 키, value = 장소명 (placeName)
    // Hash 2: field = 좌표 키, value = 주소 (address)
    // ───────────────────────────────────────────────────

    @Override
    public void saveLocation(UUID userId, Double lat, Double lng, Double radius, String placeName, String address) {
        if (lat == null || lng == null) return;
        try {
            String coordKey = locationKey(lat, lng, radius);
            String zSetKey = LOCATION_PREFIX + userId;
            String placeKey = LOCATION_PLACE_PREFIX + userId;
            String addrKey = LOCATION_ADDRESS_PREFIX + userId;

            redisTemplate.opsForZSet().add(zSetKey, coordKey, System.currentTimeMillis());
            if (placeName != null && !placeName.isBlank()) {
                redisTemplate.opsForHash().put(placeKey, coordKey, placeName);
            }
            if (address != null && !address.isBlank()) {
                redisTemplate.opsForHash().put(addrKey, coordKey, address);
            }
            trimZSet(zSetKey, placeKey, addrKey);
        } catch (Exception e) {
            log.warn("위치 검색 기록 저장 실패: userId={}", userId, e);
        }
    }

    @Override
    public List<LocationHistoryResponseDto> getLocations(UUID userId) {
        String zSetKey = LOCATION_PREFIX + userId;
        String placeKey = LOCATION_PLACE_PREFIX + userId;
        String addrKey = LOCATION_ADDRESS_PREFIX + userId;

        Set<Object> members = redisTemplate.opsForZSet().reverseRange(zSetKey, 0, -1);
        if (members == null || members.isEmpty()) return List.of();

        List<String> memberList = members.stream().map(Object::toString).toList();
        List<Object> hashKeys = memberList.stream().map(s -> (Object) s).toList();
        List<Object> placeNames = redisTemplate.opsForHash().multiGet(placeKey, hashKeys);
        List<Object> addresses = redisTemplate.opsForHash().multiGet(addrKey, hashKeys);

        List<LocationHistoryResponseDto> result = new ArrayList<>();
        for (int i = 0; i < memberList.size(); i++) {
            String member = memberList.get(i);
            String place = (placeNames.get(i) != null) ? placeNames.get(i).toString() : null;
            String addr = (addresses.get(i) != null) ? addresses.get(i).toString() : null;
            result.add(LocationHistoryResponseDto.from(member, place, addr));
        }
        return result;
    }

    @Override
    public void deleteLocation(UUID userId, String key) {
        redisTemplate.opsForZSet().remove(LOCATION_PREFIX + userId, key);
        redisTemplate.opsForHash().delete(LOCATION_PLACE_PREFIX + userId, key);
        redisTemplate.opsForHash().delete(LOCATION_ADDRESS_PREFIX + userId, key);
    }

    @Override
    public void deleteAllLocations(UUID userId) {
        redisTemplate.delete(LOCATION_PREFIX + userId);
        redisTemplate.delete(LOCATION_PLACE_PREFIX + userId);
        redisTemplate.delete(LOCATION_ADDRESS_PREFIX + userId);
    }

    // ───────────────────────────────────────────────────
    // private
    // ───────────────────────────────────────────────────

    private void trimZSet(String zSetKey, String... hashKeys) {
        Long size = redisTemplate.opsForZSet().size(zSetKey);
        if (size == null || size <= MAX_HISTORY) return;

        Set<Object> toRemove = redisTemplate.opsForZSet().range(zSetKey, 0, size - MAX_HISTORY - 1);
        if (toRemove == null || toRemove.isEmpty()) return;

        redisTemplate.opsForZSet().remove(zSetKey, toRemove.toArray());
        for (String hashKey : hashKeys) {
            if (hashKey != null) {
                redisTemplate.opsForHash().delete(hashKey, toRemove.toArray());
            }
        }
    }

    private String locationKey(Double lat, Double lng, Double radius) {
        return String.format("%.4f_%.4f_%.1f", lat, lng, radius != null ? radius : 3.0);
    }
}
