package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.finder.dto.FinderSessionListResponseDto;
import com.jiburo.server.domain.finder.service.FinderService;
import com.jiburo.server.domain.post.dto.LostPostResponseDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.jiburo.server.domain.post.service.LostPostService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.UserProfileResponseDto;
import com.jiburo.server.domain.user.dto.UserProfileUpdateRequestDto;
import com.jiburo.server.domain.user.dto.UserSettingsResponseDto;
import com.jiburo.server.domain.user.dto.UserSettingsUpdateRequestDto;
import com.jiburo.server.domain.user.service.UserService;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
@Tag(name = "User", description = "마이페이지 관련 API")
public class UserController implements UserControllerDocs {

    private final UserService userService;
    private final LostPostService lostPostService;
    private final FinderService finderService;

    @Override
    @GetMapping
    public ApiResponse<UserProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        return ApiResponse.success(userService.getMyProfile(user.getUserId()));
    }

    @Override
    @AuditLog(action = LogActionType.USER_UPDATE)
    @PatchMapping
    public ApiResponse<UserProfileResponseDto> updateMyProfile(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody UserProfileUpdateRequestDto requestDto
    ) {
        return ApiResponse.success(userService.updateMyProfile(user.getUserId(), requestDto));
    }

    @Override
    @GetMapping("/settings")
    public ApiResponse<UserSettingsResponseDto> getMySettings(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        return ApiResponse.success(userService.getMySettings(user.getUserId()));
    }

    @Override
    @PatchMapping("/settings")
    public ApiResponse<UserSettingsResponseDto> updateMySettings(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody UserSettingsUpdateRequestDto requestDto
    ) {
        return ApiResponse.success(userService.updateMySettings(user.getUserId(), requestDto));
    }

    @Override
    @GetMapping("/posts")
    public ApiResponse<Page<LostPostResponseDto>> getMyPosts(
            @AuthenticationPrincipal CustomOAuth2User user,
            @ModelAttribute LostPostSearchCondition condition,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(lostPostService.search(withMineOnly(condition), pageable, user.getUserId()));
    }

    @Override
    @GetMapping("/activities")
    public ApiResponse<Slice<FinderSessionListResponseDto>> getMyActivities(
            @AuthenticationPrincipal CustomOAuth2User user,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(finderService.getMySessions(user.getUserId(), pageable));
    }

    @Override
    @AuditLog(action = LogActionType.USER_WITHDRAW)
    @DeleteMapping
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        userService.withdraw(user.getUserId());
        return ApiResponse.success();
    }

    // 클라이언트가 mineOnly를 무엇으로 보내든 본인 게시글로 강제한다
    private LostPostSearchCondition withMineOnly(LostPostSearchCondition condition) {
        if (condition == null) {
            return new LostPostSearchCondition(null, null, null, null, null, null, true);
        }
        return new LostPostSearchCondition(
                condition.categoryCode(),
                condition.statusCode(),
                condition.keyword(),
                condition.dateFrom(),
                condition.dateTo(),
                condition.detailFilters(),
                true
        );
    }
}
