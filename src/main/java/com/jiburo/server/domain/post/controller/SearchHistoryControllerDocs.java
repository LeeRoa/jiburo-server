package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.post.dto.LocationHistoryResponseDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

public interface SearchHistoryControllerDocs {

    @Operation(summary = "키워드 검색 기록 조회", description = "로그인한 사용자의 키워드 검색 기록 목록을 조회합니다.")
    ApiResponse<List<String>> getKeywordHistory(CustomOAuth2User user);

    @Operation(summary = "키워드 검색 기록 전체 삭제", description = "로그인한 사용자의 키워드 검색 기록을 모두 삭제합니다.")
    ApiResponse<Void> deleteAllKeywordHistory(CustomOAuth2User user);

    @Operation(summary = "키워드 검색 기록 개별 삭제", description = "지정한 키워드 하나를 검색 기록에서 삭제합니다.")
    ApiResponse<Void> deleteKeywordHistory(
            CustomOAuth2User user,
            @Parameter(description = "삭제할 검색 키워드") String keyword
    );

    @Operation(summary = "위치 검색 기록 조회", description = "로그인한 사용자의 위치 검색 기록 목록을 조회합니다.")
    ApiResponse<List<LocationHistoryResponseDto>> getLocationHistory(CustomOAuth2User user);

    @Operation(summary = "위치 검색 기록 전체 삭제", description = "로그인한 사용자의 위치 검색 기록을 모두 삭제합니다.")
    ApiResponse<Void> deleteAllLocationHistory(CustomOAuth2User user);

    @Operation(summary = "위치 검색 기록 개별 삭제", description = "지정한 위치 검색 기록 하나를 삭제합니다.")
    ApiResponse<Void> deleteLocationHistory(
            CustomOAuth2User user,
            @Parameter(description = "삭제할 위치 검색 기록의 식별자. \"{lat4dp}_{lng4dp}_{radius1dp}\" 형식 (예: 37.5665_126.9780_3.0)") String key
    );

    @Operation(summary = "검색 기록 저장 기능 상태 조회", description = "로그인한 사용자의 검색 기록 저장 기능이 켜져 있는지 여부를 조회합니다.")
    ApiResponse<Boolean> getHistoryEnabled(CustomOAuth2User user);

    @Operation(summary = "검색 기록 저장 기능 토글", description = "로그인한 사용자의 검색 기록 저장 기능 상태를 반전시키고, 반전된 새 상태를 반환합니다.")
    ApiResponse<Boolean> toggleHistoryEnabled(CustomOAuth2User user);
}
