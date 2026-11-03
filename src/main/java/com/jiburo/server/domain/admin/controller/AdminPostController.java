package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminPostDtos.ForceDeleteRequest;
import com.jiburo.server.domain.admin.dto.AdminPostDtos.PostDetail;
import com.jiburo.server.domain.admin.dto.AdminPostDtos.PostSummary;
import com.jiburo.server.domain.admin.dto.AdminPostDtos.SearchCondition;
import com.jiburo.server.domain.admin.service.AdminPostService;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/posts")
@Tag(name = "Admin-Post", description = "[관리자] 게시글 관리 API")
public class AdminPostController {

    private final AdminPostService adminPostService;

    @Operation(summary = "[관리자] 게시글 검색", description = "삭제·비공개 글 포함 전체 조회 (기본: 삭제 제외, 최신순)")
    @GetMapping
    @PreAuthorize("@adminAccess.read('POST')")
    public ApiResponse<Page<PostSummary>> search(
            @ParameterObject @ModelAttribute SearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(adminPostService.search(condition, pageable));
    }

    @Operation(summary = "[관리자] 게시글 상세", description = "삭제된 글도 조회 가능")
    @GetMapping("/{postId}")
    @PreAuthorize("@adminAccess.read('POST')")
    public ApiResponse<PostDetail> find(@PathVariable String postId) {
        return ApiResponse.success(adminPostService.find(HashidsUtils.decode(postId)));
    }

    @Operation(summary = "[관리자] 게시글 강제 삭제",
            description = "작성자 삭제와 같은 Soft Delete(채팅·탐색 기록 보존, 좋아요 삭제, 진행 중 탐색 취소) + 작성자 알림. 사유 필수")
    @PostMapping("/{postId}/force-delete")
    @PreAuthorize("@adminAccess.write('POST')")
    @AuditLog(action = LogActionType.ADMIN_POST_DELETE)
    public ApiResponse<Void> forceDelete(@PathVariable String postId, @Valid @RequestBody ForceDeleteRequest request) {
        adminPostService.forceDelete(HashidsUtils.decode(postId));
        return ApiResponse.success();
    }

    @Operation(summary = "[관리자] 삭제된 게시글 복구", description = "삭제 시 정리된 좋아요·취소된 탐색 세션은 되살리지 않습니다.")
    @PostMapping("/{postId}/restore")
    @PreAuthorize("@adminAccess.write('POST')")
    @AuditLog(action = LogActionType.ADMIN_POST_RESTORE)
    public ApiResponse<Void> restore(@PathVariable String postId) {
        adminPostService.restore(HashidsUtils.decode(postId));
        return ApiResponse.success();
    }
}
