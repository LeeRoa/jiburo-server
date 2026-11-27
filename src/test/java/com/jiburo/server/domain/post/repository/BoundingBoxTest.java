package com.jiburo.server.domain.post.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BoundingBoxTest {

    // Haversine 거리 (km)
    private static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Test
    void 서울_위도에서_반경_안의_동쪽_지점이_박스에_포함된다() {
        double lat = 37.5665, lng = 126.9780, radius = 3.0;
        LostPostRepositoryImpl.BoundingBox box = LostPostRepositoryImpl.BoundingBox.of(lat, lng, radius);

        // 정동쪽으로 2.9km 떨어진 지점 (반경 안) — 보정 전에는 경도 범위가 약 2.38km라 누락됐음
        double eastLng = lng + 2.9 / (111.0 * Math.cos(Math.toRadians(lat)));
        assertThat(distanceKm(lat, lng, lat, eastLng)).isLessThan(radius);
        assertThat(eastLng).isBetween(box.minLng(), box.maxLng());
    }

    @Test
    void 위도_범위는_반경_km_나누기_111() {
        LostPostRepositoryImpl.BoundingBox box = LostPostRepositoryImpl.BoundingBox.of(37.5, 127.0, 11.1);

        assertThat(box.maxLat() - 37.5).isCloseTo(0.1, within(1e-9));
        assertThat(box.maxLng() - 127.0).isGreaterThan(0.1); // 경도 폭은 cos(위도)만큼 더 넓다
    }
}
