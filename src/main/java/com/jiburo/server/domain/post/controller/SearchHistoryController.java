package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.post.dto.LocationHistoryResponseDto;
import com.jiburo.server.domain.post.service.SearchHistoryService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "SearchHistory", description = "검색 기록 API")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    // ───────────── 키워드 검색 기록 ─────────────

    @GetMapping("/search/history")
    public ApiResponse<List<String>> getKeywordHistory(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        requireAuth(user);
        return ApiResponse.success(searchHistoryService.getKeywords(user.getUserId()));
    }

    @DeleteMapping("/search/history")
    public ApiResponse<Void> deleteAllKeywordHistory(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        requireAuth(user);
        searchHistoryService.deleteAllKeywords(user.getUserId());
        return ApiResponse.success();
    }

    @DeleteMapping("/search/history/{keyword}")
    public ApiResponse<Void> deleteKeywordHistory(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String keyword
    ) {
        requireAuth(user);
        searchHistoryService.deleteKeyword(user.getUserId(), keyword);
        return ApiResponse.success();
    }

    // ───────────── 위치 검색 기록 ─────────────

    @GetMapping("/location-history")
    public ApiResponse<List<LocationHistoryResponseDto>> getLocationHistory(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        requireAuth(user);
        return ApiResponse.success(searchHistoryService.getLocations(user.getUserId()));
    }

    @DeleteMapping("/location-history")
    public ApiResponse<Void> deleteAllLocationHistory(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        requireAuth(user);
        searchHistoryService.deleteAllLocations(user.getUserId());
        return ApiResponse.success();
    }

    // key 형식: "{lat4dp}_{lng4dp}_{radius1dp}" (예: 37.5665_126.9780_3.0)
    @DeleteMapping("/location-history/{key}")
    public ApiResponse<Void> deleteLocationHistory(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String key
    ) {
        requireAuth(user);
        searchHistoryService.deleteLocation(user.getUserId(), key);
        return ApiResponse.success();
    }

    // ───────────── 검색 기록 활성/비활성 설정 ─────────────

    @GetMapping("/search/history/enabled")
    public ApiResponse<Boolean> getHistoryEnabled(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        requireAuth(user);
        return ApiResponse.success(searchHistoryService.isHistoryEnabled(user.getUserId()));
    }

    // 현재 상태를 반전시키고 새 상태를 반환
    @PostMapping("/search/history/toggle")
    public ApiResponse<Boolean> toggleHistoryEnabled(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        requireAuth(user);
        return ApiResponse.success(searchHistoryService.toggleHistoryEnabled(user.getUserId()));
    }

    private void requireAuth(CustomOAuth2User user) {
        if (user == null) throw new JiburoException(ErrorCode.UNAUTHORIZED);
    }
}
