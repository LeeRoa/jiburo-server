package com.jiburo.server.domain.finder.controller;

import com.jiburo.server.domain.finder.dto.*;
import com.jiburo.server.domain.finder.service.FinderService;
import java.util.List;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finder/sessions")
@Tag(name = "FinderSession", description = "파인더 탐색 세션 API")
public class FinderController implements FinderControllerDocs {

    private final FinderService finderService;

    @Override
    @AuditLog(action = LogActionType.FINDER_SESSION_CREATE)
    @PostMapping
    public ApiResponse<String> createSession(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody FinderSessionCreateRequestDto requestDto
    ) {
        return ApiResponse.success(finderService.createSession(user.getUserId(), requestDto));
    }

    @Override
    @GetMapping
    public ApiResponse<Slice<FinderSessionListResponseDto>> getMySessions(
            @AuthenticationPrincipal CustomOAuth2User user,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(finderService.getMySessions(user.getUserId(), pageable));
    }

    @Override
    @GetMapping("/{sessionId}")
    public ApiResponse<FinderSessionResponseDto> getSession(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId
    ) {
        Long realId = HashidsUtils.decode(sessionId);
        return ApiResponse.success(finderService.getSession(user.getUserId(), realId));
    }

    @Override
    @PostMapping("/{sessionId}/media")
    public ApiResponse<List<FinderMediaResponseDto>> addMedia(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId,
            @RequestBody FinderMediaAddRequestDto requestDto
    ) {
        Long realId = HashidsUtils.decode(sessionId);
        return ApiResponse.success(finderService.addMedia(user.getUserId(), realId, requestDto));
    }

    @Override
    @DeleteMapping("/{sessionId}/media/{mediaId}")
    public ApiResponse<Void> deleteMedia(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId,
            @PathVariable String mediaId
    ) {
        Long realSessionId = HashidsUtils.decode(sessionId);
        Long realMediaId = HashidsUtils.decode(mediaId);
        finderService.deleteMedia(user.getUserId(), realSessionId, realMediaId);
        return ApiResponse.success();
    }

    @Override
    @AuditLog(action = LogActionType.FINDER_SESSION_SUBMIT)
    @PostMapping("/{sessionId}/submit")
    public ApiResponse<Void> submitSession(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId,
            @Valid @RequestBody FinderSessionSubmitRequestDto requestDto
    ) {
        Long realId = HashidsUtils.decode(sessionId);
        finderService.submitSession(user.getUserId(), realId, requestDto);
        return ApiResponse.success();
    }

    @Override
    @PostMapping("/{sessionId}/route")
    public ApiResponse<Void> recordRoute(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId,
            @Valid @RequestBody FinderRouteRecordRequestDto requestDto
    ) {
        Long realId = HashidsUtils.decode(sessionId);
        finderService.recordRoute(user.getUserId(), realId, requestDto);
        return ApiResponse.success();
    }

    @Override
    @GetMapping("/{sessionId}/route")
    public ApiResponse<List<FinderRoutePointResponseDto>> getRoute(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId
    ) {
        Long realId = HashidsUtils.decode(sessionId);
        return ApiResponse.success(finderService.getRoute(user.getUserId(), realId));
    }

    @Override
    @AuditLog(action = LogActionType.FINDER_SESSION_CANCEL)
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> cancelSession(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String sessionId
    ) {
        Long realId = HashidsUtils.decode(sessionId);
        finderService.cancelSession(user.getUserId(), realId);
        return ApiResponse.success();
    }
}
