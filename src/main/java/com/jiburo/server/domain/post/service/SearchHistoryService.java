package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.dto.LocationHistoryResponseDto;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryService {

    void saveKeyword(UUID userId, String keyword);
    List<String> getKeywords(UUID userId);
    void deleteKeyword(UUID userId, String keyword);
    void deleteAllKeywords(UUID userId);

    void saveLocation(UUID userId, Double lat, Double lng, Double radius, String placeName, String address);
    List<LocationHistoryResponseDto> getLocations(UUID userId);
    void deleteLocation(UUID userId, String key);
    void deleteAllLocations(UUID userId);
}
