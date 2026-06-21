package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.dto.LocationHistoryResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchHistoryServiceTest {

    @InjectMocks
    private SearchHistoryServiceImpl searchHistoryService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOps;

    @Mock
    private HashOperations hashOps;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        // generic 반환 타입 메서드는 given() 대신 doReturn() 사용
        doReturn(zSetOps).when(redisTemplate).opsForZSet();
        doReturn(hashOps).when(redisTemplate).opsForHash();
        doReturn(valueOps).when(redisTemplate).opsForValue();
        // 기본값: 검색 기록 활성 상태 (키 없음 = null → true)
        given(valueOps.get(anyString())).willReturn(null);
    }

    // ───────────────────────────────────────────────────
    // saveKeyword
    // ───────────────────────────────────────────────────

    @Test
    void saveKeyword_정상_ZSet에_저장() {
        // given
        given(zSetOps.size(anyString())).willReturn(1L);

        // when
        searchHistoryService.saveKeyword(userId, "강아지");

        // then
        then(zSetOps).should().add(
                eq("search:keyword:" + userId),
                eq("강아지"),
                anyDouble()
        );
    }

    @Test
    void saveKeyword_blank이면_저장_안함() {
        // when
        searchHistoryService.saveKeyword(userId, "  ");

        // then
        then(zSetOps).should(never()).add(anyString(), any(), anyDouble());
    }

    @Test
    void saveKeyword_null이면_저장_안함() {
        // when
        searchHistoryService.saveKeyword(userId, null);

        // then
        then(zSetOps).should(never()).add(anyString(), any(), anyDouble());
    }

    @Test
    void saveKeyword_10개_초과_시_가장_오래된_항목_제거() {
        // given: size가 11이면 0번 인덱스(가장 오래된) 제거
        given(zSetOps.size(anyString())).willReturn(11L);
        given(zSetOps.range(anyString(), eq(0L), eq(0L))).willReturn(Set.of("오래된키워드"));

        // when
        searchHistoryService.saveKeyword(userId, "새키워드");

        // then
        then(zSetOps).should().remove(anyString(), any(Object[].class));
    }

    @Test
    void saveKeyword_10개_이하면_트림_안함() {
        // given
        given(zSetOps.size(anyString())).willReturn(10L);

        // when
        searchHistoryService.saveKeyword(userId, "강아지");

        // then
        then(zSetOps).should(never()).remove(anyString(), any(Object[].class));
    }

    @Test
    void saveKeyword_Redis_오류_시_예외_삼켜짐() {
        // given: Redis 장애 상황
        given(zSetOps.add(anyString(), any(), anyDouble())).willThrow(new RuntimeException("Redis 연결 실패"));

        // when & then: 예외가 전파되지 않아야 함
        searchHistoryService.saveKeyword(userId, "강아지");
    }

    // ───────────────────────────────────────────────────
    // getKeywords
    // ───────────────────────────────────────────────────

    @Test
    void getKeywords_목록_최신순_반환() {
        // given
        given(zSetOps.reverseRange(anyString(), eq(0L), eq(-1L)))
                .willReturn(Set.of("강아지", "고양이"));

        // when
        List<String> result = searchHistoryService.getKeywords(userId);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).containsExactlyInAnyOrder("강아지", "고양이");
    }

    @Test
    void getKeywords_Redis_결과_null이면_빈_리스트() {
        // given
        given(zSetOps.reverseRange(anyString(), anyLong(), anyLong())).willReturn(null);

        // when
        List<String> result = searchHistoryService.getKeywords(userId);

        // then
        assertThat(result).isEmpty();
    }

    // ───────────────────────────────────────────────────
    // deleteKeyword
    // ───────────────────────────────────────────────────

    @Test
    void deleteKeyword_ZSet에서_제거() {
        // when
        searchHistoryService.deleteKeyword(userId, "강아지");

        // then
        then(zSetOps).should().remove("search:keyword:" + userId, "강아지");
    }

    // ───────────────────────────────────────────────────
    // deleteAllKeywords
    // ───────────────────────────────────────────────────

    @Test
    void deleteAllKeywords_키_삭제() {
        // when
        searchHistoryService.deleteAllKeywords(userId);

        // then
        then(redisTemplate).should().delete("search:keyword:" + userId);
    }

    // ───────────────────────────────────────────────────
    // saveLocation
    // ───────────────────────────────────────────────────

    @Test
    void saveLocation_정상_ZSet과_Hash에_저장() {
        // given
        given(zSetOps.size(anyString())).willReturn(1L);

        // when
        searchHistoryService.saveLocation(userId, 37.5665, 126.9780, 3.0, "서울역", "서울 중구 세종대로 1");

        // then: ZSet에 좌표 키 저장
        then(zSetOps).should().add(
                eq("search:location:" + userId),
                eq("37.5665_126.9780_3.0"),
                anyDouble()
        );
        // then: placeName Hash에 저장
        then(hashOps).should().put(
                eq("search:location-place:" + userId),
                eq("37.5665_126.9780_3.0"),
                eq("서울역")
        );
        // then: address Hash에 저장
        then(hashOps).should().put(
                eq("search:location-address:" + userId),
                eq("37.5665_126.9780_3.0"),
                eq("서울 중구 세종대로 1")
        );
    }

    @Test
    void saveLocation_placeName_null이면_place_Hash_미저장() {
        // given
        given(zSetOps.size(anyString())).willReturn(1L);

        // when
        searchHistoryService.saveLocation(userId, 37.5665, 126.9780, 3.0, null, null);

        // then: ZSet은 저장, Hash는 저장 안 함
        then(zSetOps).should().add(anyString(), any(), anyDouble());
        then(hashOps).should(never()).put(anyString(), any(), any());
    }

    @Test
    void saveLocation_lat_null이면_저장_안함() {
        // when
        searchHistoryService.saveLocation(userId, null, 126.9780, 3.0, "서울역", "서울 중구 세종대로 1");

        // then
        then(zSetOps).should(never()).add(anyString(), any(), anyDouble());
    }

    @Test
    void saveLocation_10개_초과_시_ZSet과_Hash_모두_트림() {
        // given
        given(zSetOps.size(anyString())).willReturn(11L);
        given(zSetOps.range(anyString(), eq(0L), eq(0L))).willReturn(Set.of("37.0000_127.0000_3.0"));

        // when
        searchHistoryService.saveLocation(userId, 37.5665, 126.9780, 3.0, "서울역", "서울 중구 세종대로 1");

        // then: ZSet, Hash(place+address) 모두 오래된 항목 제거
        then(zSetOps).should().remove(anyString(), any(Object[].class));
        then(hashOps).should(org.mockito.Mockito.atLeast(2)).delete(anyString(), any(Object[].class));
    }

    @Test
    void saveLocation_Redis_오류_시_예외_삼켜짐() {
        // given
        given(zSetOps.add(anyString(), any(), anyDouble())).willThrow(new RuntimeException("Redis 연결 실패"));

        // when & then: 예외가 전파되지 않아야 함
        searchHistoryService.saveLocation(userId, 37.5665, 126.9780, 3.0, "서울역", "서울 중구 세종대로 1");
    }

    // ───────────────────────────────────────────────────
    // getLocations
    // ───────────────────────────────────────────────────

    @Test
    void getLocations_placeName_address_포함_목록_반환() {
        // given
        given(zSetOps.reverseRange(anyString(), eq(0L), eq(-1L)))
                .willReturn(Set.of("37.5665_126.9780_3.0"));
        // place hash 조회, address hash 조회 순서로 두 번 호출됨
        given(hashOps.multiGet(anyString(), anyList()))
                .willReturn(List.of("서울역"))
                .willReturn(List.of("서울 중구 세종대로 1"));

        // when
        List<LocationHistoryResponseDto> result = searchHistoryService.getLocations(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).lat()).isEqualTo(37.5665);
        assertThat(result.get(0).lng()).isEqualTo(126.9780);
        assertThat(result.get(0).radius()).isEqualTo(3.0);
        assertThat(result.get(0).placeName()).isEqualTo("서울역");
        assertThat(result.get(0).address()).isEqualTo("서울 중구 세종대로 1");
        assertThat(result.get(0).key()).isEqualTo("37.5665_126.9780_3.0");
    }

    @Test
    void getLocations_placeName_null인_경우_null로_반환() {
        // given: placeName, address 모두 저장되지 않은 위치
        given(zSetOps.reverseRange(anyString(), eq(0L), eq(-1L)))
                .willReturn(Set.of("37.5665_126.9780_3.0"));
        given(hashOps.multiGet(anyString(), anyList()))
                .willReturn(Collections.singletonList(null))
                .willReturn(Collections.singletonList(null));

        // when
        List<LocationHistoryResponseDto> result = searchHistoryService.getLocations(userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).placeName()).isNull();
        assertThat(result.get(0).address()).isNull();
    }

    @Test
    void getLocations_Redis_결과_null이면_빈_리스트() {
        // given
        given(zSetOps.reverseRange(anyString(), anyLong(), anyLong())).willReturn(null);

        // when
        List<LocationHistoryResponseDto> result = searchHistoryService.getLocations(userId);

        // then
        assertThat(result).isEmpty();
    }

    // ───────────────────────────────────────────────────
    // deleteLocation
    // ───────────────────────────────────────────────────

    @Test
    void deleteLocation_ZSet과_Hash_동시_삭제() {
        // given
        String key = "37.5665_126.9780_3.0";

        // when
        searchHistoryService.deleteLocation(userId, key);

        // then
        then(zSetOps).should().remove("search:location:" + userId, key);
        then(hashOps).should().delete("search:location-place:" + userId, key);
        then(hashOps).should().delete("search:location-address:" + userId, key);
    }

    // ───────────────────────────────────────────────────
    // deleteAllLocations
    // ───────────────────────────────────────────────────

    @Test
    void deleteAllLocations_ZSet과_Hash_키_모두_삭제() {
        // when
        searchHistoryService.deleteAllLocations(userId);

        // then
        then(redisTemplate).should().delete("search:location:" + userId);
        then(redisTemplate).should().delete("search:location-place:" + userId);
        then(redisTemplate).should().delete("search:location-address:" + userId);
    }

    // ───────────────────────────────────────────────────
    // isHistoryEnabled
    // ───────────────────────────────────────────────────

    @Test
    void isHistoryEnabled_Redis에_값_없으면_기본값_활성_반환() {
        // given: 값 없음(null) = 기본 활성
        given(valueOps.get(anyString())).willReturn(null);

        // when & then
        assertThat(searchHistoryService.isHistoryEnabled(userId)).isTrue();
    }

    @Test
    void isHistoryEnabled_false_저장되면_비활성_반환() {
        // given
        given(valueOps.get("search:history:enabled:" + userId)).willReturn("false");

        // when & then
        assertThat(searchHistoryService.isHistoryEnabled(userId)).isFalse();
    }

    @Test
    void isHistoryEnabled_true_저장되면_활성_반환() {
        // given
        given(valueOps.get("search:history:enabled:" + userId)).willReturn("true");

        // when & then
        assertThat(searchHistoryService.isHistoryEnabled(userId)).isTrue();
    }

    // ───────────────────────────────────────────────────
    // toggleHistoryEnabled
    // ───────────────────────────────────────────────────

    @Test
    void toggleHistoryEnabled_활성_상태에서_토글하면_비활성_반환_후_false_저장() {
        // given: 현재 활성 (null → true)
        given(valueOps.get(anyString())).willReturn(null);

        // when
        boolean result = searchHistoryService.toggleHistoryEnabled(userId);

        // then
        assertThat(result).isFalse();
        then(valueOps).should().set("search:history:enabled:" + userId, "false");
    }

    @Test
    void toggleHistoryEnabled_비활성_상태에서_토글하면_활성_반환_후_true_저장() {
        // given: 현재 비활성
        given(valueOps.get(anyString())).willReturn("false");

        // when
        boolean result = searchHistoryService.toggleHistoryEnabled(userId);

        // then
        assertThat(result).isTrue();
        then(valueOps).should().set("search:history:enabled:" + userId, "true");
    }

    // ───────────────────────────────────────────────────
    // 비활성 시 저장 차단
    // ───────────────────────────────────────────────────

    @Test
    void saveKeyword_검색기록_비활성_상태이면_저장_안함() {
        // given: 비활성 상태
        given(valueOps.get(anyString())).willReturn("false");

        // when
        searchHistoryService.saveKeyword(userId, "강아지");

        // then
        then(zSetOps).should(never()).add(anyString(), any(), anyDouble());
    }

    @Test
    void saveLocation_검색기록_비활성_상태이면_저장_안함() {
        // given: 비활성 상태
        given(valueOps.get(anyString())).willReturn("false");

        // when
        searchHistoryService.saveLocation(userId, 37.5665, 126.9780, 3.0, "서울역", "서울 중구 세종대로 1");

        // then
        then(zSetOps).should(never()).add(anyString(), any(), anyDouble());
    }
}
