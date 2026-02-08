package com.jiburo.server.domain.post.controller;

import com.jiburo.server.domain.post.dto.*;
import com.jiburo.server.domain.post.service.LostPostService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class LostPostController {

    private final LostPostService lostPostService;

    // [등록]
    @AuditLog(action = CodeConst.LogAction.POST_CREATE)
    @PostMapping
    public ApiResponse<Long> createPost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody LostPostCreateRequestDto requestDto
    ) {
        Long postId = lostPostService.create(user.getUserId(), requestDto);
        return ApiResponse.success(postId);
    }

    // [전체 조회]
    @GetMapping
    public ApiResponse<Page<LostPostResponseDto>> getPosts(
            @ModelAttribute LostPostSearchCondition condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(lostPostService.search(condition, pageable));
    }

    // [단건 조회]
    @GetMapping("/{id}")
    public ApiResponse<LostPostResponseDto> getPost(@PathVariable Long id) {
        return ApiResponse.success(lostPostService.findById(id));
    }

    // [수정]
    @AuditLog(action = CodeConst.LogAction.POST_UPDATE)
    @PatchMapping("/{id}")
    public ApiResponse<Void> updatePost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long id,
            @RequestBody LostPostUpdateRequestDto requestDto
    ) {
        lostPostService.update(user.getUserId(), id, requestDto);
        return ApiResponse.success();
    }

    // [상태 변경]
    @AuditLog(action = CodeConst.LogAction.POST_STATUS_CHANGE)
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long id,
            @RequestParam String statusCode // 파라미터명 statusCode로 명시
    ) {
        lostPostService.updateStatus(user.getUserId(), id, statusCode);
        return ApiResponse.success();
    }

    // [삭제]
    @AuditLog(action = CodeConst.LogAction.POST_DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable Long id
    ) {
        lostPostService.delete(user.getUserId(), id);
        return ApiResponse.success();
    }

    // 지도 이동 시 호출 (마커용)
    @GetMapping("/map")
    public ApiResponse<List<LostPostResponseDto>> getMapPosts(@ModelAttribute LostPostMapRequestDto request) {
        return ApiResponse.success(lostPostService.getPostsForMap(request));
    }

    // 마커 클릭 시 호출 (리스트용)
    @GetMapping("/nearby")
    public ApiResponse<List<LostPostResponseDto>> getNearbyPosts(@ModelAttribute LostPostNearbyRequestDto request) {
        return ApiResponse.success(lostPostService.getPostsForList(request));
    }
}
