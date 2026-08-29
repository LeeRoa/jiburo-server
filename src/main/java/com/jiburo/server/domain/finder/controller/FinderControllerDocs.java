package com.jiburo.server.domain.finder.controller;

import com.jiburo.server.domain.finder.dto.FinderMediaAddRequestDto;
import com.jiburo.server.domain.finder.dto.FinderMediaResponseDto;
import com.jiburo.server.domain.finder.dto.FinderRoutePointResponseDto;
import com.jiburo.server.domain.finder.dto.FinderRouteRecordRequestDto;
import com.jiburo.server.domain.finder.dto.FinderSessionCreateRequestDto;
import com.jiburo.server.domain.finder.dto.FinderSessionListResponseDto;
import com.jiburo.server.domain.finder.dto.FinderSessionResponseDto;
import com.jiburo.server.domain.finder.dto.FinderSessionSubmitRequestDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface FinderControllerDocs {

    @Operation(summary = "탐색 세션 시작", description = "실종 게시글에 대한 파인더 탐색 세션을 시작합니다. 탐색을 시작한 위치(위도/경도)를 함께 기록합니다.")
    ApiResponse<String> createSession(
            CustomOAuth2User user,
            FinderSessionCreateRequestDto requestDto
    );

    @Operation(summary = "내 탐색 세션 목록 조회", description = "로그인한 사용자가 진행했거나 진행 중인 파인더 탐색 세션 목록을 페이징 조회합니다.")
    ApiResponse<Slice<FinderSessionListResponseDto>> getMySessions(
            CustomOAuth2User user,
            Pageable pageable
    );

    @Operation(summary = "탐색 세션 상세 조회", description = "탐색 세션의 상세 정보(대상 게시글, 상태, 결과, 제보 미디어 목록 등)를 조회합니다.")
    ApiResponse<FinderSessionResponseDto> getSession(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId
    );

    @Operation(summary = "탐색 제보 미디어 추가", description = "탐색 중 발견한 사진/텍스트 제보(미디어)를 세션에 추가합니다.")
    ApiResponse<List<FinderMediaResponseDto>> addMedia(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId,
            FinderMediaAddRequestDto requestDto
    );

    @Operation(summary = "탐색 제보 미디어 삭제", description = "탐색 세션에 추가된 제보 미디어를 삭제합니다.")
    ApiResponse<Void> deleteMedia(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId,
            @Parameter(description = "제보 미디어 ID (인코딩된 문자열)") String mediaId
    );

    @Operation(summary = "탐색 결과 제출", description = "탐색 세션의 결과(찾음/못 찾음 등)와 메모를 제출하여 세션을 종료합니다.")
    ApiResponse<Void> submitSession(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId,
            FinderSessionSubmitRequestDto requestDto
    );

    @Operation(summary = "탐색 이동 경로 기록", description = "탐색 중 이동한 GPS 좌표 목록을 세션에 기록합니다.")
    ApiResponse<Void> recordRoute(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId,
            FinderRouteRecordRequestDto requestDto
    );

    @Operation(summary = "탐색 이동 경로 조회", description = "탐색 세션에 기록된 GPS 이동 경로 목록을 조회합니다.")
    ApiResponse<List<FinderRoutePointResponseDto>> getRoute(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId
    );

    @Operation(summary = "탐색 세션 취소", description = "진행 중인 파인더 탐색 세션을 취소합니다.")
    ApiResponse<Void> cancelSession(
            CustomOAuth2User user,
            @Parameter(description = "탐색 세션 ID (인코딩된 문자열)") String sessionId
    );
}
