package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.finder.dto.FinderSessionResponseDto;
import com.jiburo.server.domain.post.dto.LostPostCreateRequestDto;
import com.jiburo.server.domain.post.dto.LostPostMapRequestDto;
import com.jiburo.server.domain.post.dto.LostPostNearbyRequestDto;
import com.jiburo.server.domain.post.dto.LostPostResponseDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.jiburo.server.domain.post.dto.LostPostStatusUpdateRequestDto;
import com.jiburo.server.domain.post.dto.LostPostUpdateRequestDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface LostPostControllerDocs {

    @Operation(summary = "분실 게시글 등록", description = "분실/제보 게시글을 등록합니다. 카테고리(categoryCode)에 따라 detail 상세 정보의 구조가 달라집니다.")
    ApiResponse<String> createPost(CustomOAuth2User user, LostPostCreateRequestDto requestDto);

    @Operation(summary = "분실 게시글 검색", description = "키워드, 카테고리, 상태, 기간, 상세 필터 조건으로 게시글을 페이지 단위로 검색합니다.")
    ApiResponse<Page<LostPostResponseDto>> getPosts(LostPostSearchCondition condition, Pageable pageable, CustomOAuth2User user);

    @Operation(summary = "분실 게시글 단건 조회", description = "게시글 ID로 상세 정보를 조회합니다.")
    ApiResponse<LostPostResponseDto> getPost(
            @Parameter(description = "게시글 ID (인코딩된 문자열)") String id,
            CustomOAuth2User user
    );

    @Operation(summary = "관심 게시글 토글", description = "게시글에 대한 좋아요(관심 등록)를 토글합니다. 응답 값이 true면 좋아요, false면 취소 상태입니다.")
    ApiResponse<Boolean> toggleLike(
            CustomOAuth2User user,
            @Parameter(description = "게시글 ID (인코딩된 문자열)") String id
    );

    @Operation(summary = "관심 게시글 목록 조회", description = "로그인한 사용자가 좋아요(관심 등록)한 게시글 목록을 페이지 단위로 조회합니다.")
    ApiResponse<Page<LostPostResponseDto>> getLikedPosts(CustomOAuth2User user, Pageable pageable);

    @Operation(summary = "분실 게시글 수정", description = "게시글을 부분 수정합니다. 요청 필드 중 null인 값은 기존 값을 유지합니다.")
    ApiResponse<Void> updatePost(
            CustomOAuth2User user,
            @Parameter(description = "게시글 ID (인코딩된 문자열)") String id,
            LostPostUpdateRequestDto requestDto
    );

    @Operation(summary = "분실 게시글 상태 변경", description = "게시글의 상태를 변경합니다 (예: 실종 중 → 보호 중/완료). 해결 처리 시 발견자(finderId), 해결 메모(resultNote)를 함께 전달할 수 있습니다.")
    ApiResponse<Void> updateStatus(
            CustomOAuth2User user,
            @Parameter(description = "게시글 ID (인코딩된 문자열)") String id,
            LostPostStatusUpdateRequestDto requestDto
    );

    @Operation(summary = "분실 게시글 삭제", description = "작성자 본인의 게시글을 삭제합니다.")
    ApiResponse<Void> deletePost(
            CustomOAuth2User user,
            @Parameter(description = "게시글 ID (인코딩된 문자열)") String id
    );

    @Operation(summary = "지도 마커용 게시글 조회", description = "지도 뷰포트(좌상단/우하단 좌표) 범위 내의 게시글 목록을 마커 표시용으로 조회합니다.")
    ApiResponse<List<LostPostResponseDto>> getMapPosts(LostPostMapRequestDto request);

    @Operation(summary = "게시글 파인더 탐색 세션 목록 조회", description = "게시글 작성자가 해당 게시글에 대해 진행된 파인더(발견 도우미) 탐색 세션 목록을 조회합니다.")
    ApiResponse<Slice<FinderSessionResponseDto>> getFinderSessions(
            CustomOAuth2User user,
            @Parameter(description = "게시글 ID (인코딩된 문자열)") String id,
            Pageable pageable
    );

    @Operation(summary = "반경 내 게시글 목록 조회", description = "지도 마커 클릭 시 호출됩니다. 중심 좌표(centerLat/centerLng) 기준 반경(radius, km) 내의 게시글을 거리순 또는 최신순으로 조회합니다.")
    ApiResponse<Slice<LostPostResponseDto>> getNearbyPosts(LostPostNearbyRequestDto request, CustomOAuth2User user);
}
