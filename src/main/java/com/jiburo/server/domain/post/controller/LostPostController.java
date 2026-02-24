package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.service.LostPostService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.CodeConst;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(name = "LostPost", description = "분실 게시글 관련 API")
public class LostPostController {

    private final LostPostService lostPostService;

    // [등록]
    @AuditLog(action = CodeConst.LogAction.POST_CREATE)
    @PostMapping
    public ApiResponse<String> createPost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody LostPostCreateRequestDto requestDto
    ) {

        String postId = HashidsUtils.encode(lostPostService.create(user.getUserId(), requestDto));
        return ApiResponse.success(postId);
    }

    // [전체 조회]
    @GetMapping("/search")
    public ApiResponse<Page<LostPostResponseDto>> getPosts(
            @ModelAttribute LostPostSearchCondition condition,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(lostPostService.search(condition, pageable));
    }

    // [단건 조회]
    @GetMapping("/{id}")
    public ApiResponse<LostPostResponseDto> getPost(@PathVariable String id) {
        Long realId = HashidsUtils.decode(id);
        return ApiResponse.success(lostPostService.findById(realId));
    }

    // [수정]
    @AuditLog(action = CodeConst.LogAction.POST_UPDATE)
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
    @AuditLog(action = CodeConst.LogAction.POST_STATUS_CHANGE)
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
    @AuditLog(action = CodeConst.LogAction.POST_DELETE)
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
    @GetMapping("/map")
    public ApiResponse<List<LostPostResponseDto>> getMapPosts(@ModelAttribute LostPostMapRequestDto request) {
        return ApiResponse.success(lostPostService.getPostsForMap(request));
    }

    // 마커 클릭 시 호출 (리스트용)
    @GetMapping("/nearby")
    public ApiResponse<Slice<LostPostResponseDto>> getNearbyPosts(@ModelAttribute LostPostNearbyRequestDto request) {
        return ApiResponse.success(lostPostService.getPostsForList(request));
    }
}
