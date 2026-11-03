package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.finder.dto.FinderSessionResponseDto;
import com.jiburo.server.domain.finder.service.FinderService;
import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.service.LostPostService;
import com.jiburo.server.domain.post.service.SearchHistoryService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(name = "LostPost", description = "분실 게시글 관련 API")
public class LostPostController implements LostPostControllerDocs {

    private final LostPostService lostPostService;
    private final SearchHistoryService searchHistoryService;
    private final FinderService finderService;

    // [등록]
    @Override
    @AuditLog(action = LogActionType.POST_CREATE)
    @PostMapping
    public ApiResponse<String> createPost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody LostPostCreateRequestDto requestDto
    ) {

        String postId = HashidsUtils.encode(lostPostService.create(user.getUserId(), requestDto));
        return ApiResponse.success(postId);
    }

    // [전체 조회]
    @Override
    @GetMapping("/search")
    public ApiResponse<Page<LostPostResponseDto>> getPosts(
            @ModelAttribute LostPostSearchCondition condition,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        UUID userId = user != null ? user.getUserId() : null;
        if (userId != null && condition.keyword() != null && !condition.keyword().isBlank()) {
            searchHistoryService.saveKeyword(userId, condition.keyword());
        }
        return ApiResponse.success(lostPostService.search(condition, pageable, userId));
    }

    // [단건 조회]
    @Override
    @GetMapping("/{id}")
    public ApiResponse<LostPostResponseDto> getPost(
            @PathVariable String id,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        Long realId = HashidsUtils.decode(id);
        UUID userId = user != null ? user.getUserId() : null;
        return ApiResponse.success(lostPostService.findById(realId, userId));
    }

    // [좋아요 토글] - liked: true(좋아요), false(취소)
    @Override
    @PostMapping("/{id}/like")
    public ApiResponse<Boolean> toggleLike(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String id
    ) {
        if (user == null) throw new JiburoException(ErrorCode.UNAUTHORIZED);
        Long realId = HashidsUtils.decode(id);
        return ApiResponse.success(lostPostService.toggleLike(user.getUserId(), realId));
    }

    // [관심 게시글 목록]
    @Override
    @GetMapping("/liked")
    public ApiResponse<Page<LostPostResponseDto>> getLikedPosts(
            @AuthenticationPrincipal CustomOAuth2User user,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (user == null) throw new JiburoException(ErrorCode.UNAUTHORIZED);
        return ApiResponse.success(lostPostService.getLikedPosts(user.getUserId(), pageable));
    }

    // [수정]
    @Override
    @AuditLog(action = LogActionType.POST_UPDATE)
    @PatchMapping("/{id}")
    public ApiResponse<Void> updatePost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String id,
            @RequestBody LostPostUpdateRequestDto requestDto
    ) {
        Long realId = HashidsUtils.decode(id);
        lostPostService.update(user.getUserId(), realId, requestDto);
        return ApiResponse.success();
    }

    // [상태 변경]
    @Override
    @AuditLog(action = LogActionType.POST_STATUS_CHANGE)
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String id,
            @RequestBody @Valid LostPostStatusUpdateRequestDto requestDto
    ) {
        Long realId = HashidsUtils.decode(id);
        lostPostService.updateStatus(user.getUserId(), realId, requestDto);
        return ApiResponse.success();
    }

    // [삭제]
    @Override
    @AuditLog(action = LogActionType.POST_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String id
    ) {
        Long realId = HashidsUtils.decode(id);
        lostPostService.delete(user.getUserId(), realId);
        return ApiResponse.success();
    }

    // 지도 이동 시 호출 (마커용)
    @Override
    @GetMapping("/map")
    public ApiResponse<List<LostPostResponseDto>> getMapPosts(
            @ModelAttribute LostPostMapRequestDto request,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        UUID viewerId = user != null ? user.getUserId() : null;
        return ApiResponse.success(lostPostService.getPostsForMap(request, viewerId));
    }

    // 게시글 작성자용: 해당 게시글의 파인더 탐색 세션 목록
    @Override
    @GetMapping("/{id}/finder-sessions")
    public ApiResponse<Slice<FinderSessionResponseDto>> getFinderSessions(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String id,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (user == null) throw new JiburoException(ErrorCode.UNAUTHORIZED);
        Long postId = HashidsUtils.decode(id);
        return ApiResponse.success(finderService.getSessionsByPost(user.getUserId(), postId, pageable));
    }

    // 마커 클릭 시 호출 (리스트용)
    @Override
    @GetMapping("/nearby")
    public ApiResponse<Slice<LostPostResponseDto>> getNearbyPosts(
            @ModelAttribute LostPostNearbyRequestDto request,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        if (user != null) {
            searchHistoryService.saveLocation(user.getUserId(), request.centerLat(), request.centerLng(), request.radius(), request.placeName(), request.address());
        }
        UUID viewerId = user != null ? user.getUserId() : null;
        return ApiResponse.success(lostPostService.getPostsForList(request, viewerId));
    }
}
