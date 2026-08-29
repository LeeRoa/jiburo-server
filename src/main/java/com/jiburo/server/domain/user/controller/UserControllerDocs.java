package com.jiburo.server.domain.user.controller;

import com.jiburo.server.domain.finder.dto.FinderSessionListResponseDto;
import com.jiburo.server.domain.post.dto.LostPostResponseDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.UserProfileResponseDto;
import com.jiburo.server.domain.user.dto.UserProfileUpdateRequestDto;
import com.jiburo.server.domain.user.dto.UserSettingsResponseDto;
import com.jiburo.server.domain.user.dto.UserSettingsUpdateRequestDto;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface UserControllerDocs {

    @Operation(summary = "내 프로필 조회", description = "마이페이지 상단에 표시할 닉네임, 프로필 이미지, 로그인 수단, 등급, 누적 활동 점수를 조회합니다.")
    ApiResponse<UserProfileResponseDto> getMyProfile(CustomOAuth2User user);

    @Operation(summary = "내 프로필 수정", description = "닉네임과 프로필 이미지를 수정합니다. null인 필드는 기존 값을 유지하며, 닉네임 중복 시 409를 반환합니다.")
    ApiResponse<UserProfileResponseDto> updateMyProfile(CustomOAuth2User user, UserProfileUpdateRequestDto requestDto);

    @Operation(summary = "내 알림/위치 설정 조회", description = "알림 수신 여부와 위치 기반 서비스 수신 여부를 조회합니다.")
    ApiResponse<UserSettingsResponseDto> getMySettings(CustomOAuth2User user);

    @Operation(summary = "내 알림/위치 설정 변경", description = "알림 수신 여부와 위치 기반 서비스 수신 여부를 변경합니다. null인 필드는 기존 값을 유지합니다.")
    ApiResponse<UserSettingsResponseDto> updateMySettings(CustomOAuth2User user, UserSettingsUpdateRequestDto requestDto);

    @Operation(summary = "내 게시물 목록 조회", description = "로그인한 사용자가 작성한 게시글을 조회합니다. statusCode로 찾는 중/완료를 필터링할 수 있습니다.")
    ApiResponse<Page<LostPostResponseDto>> getMyPosts(CustomOAuth2User user, LostPostSearchCondition condition, Pageable pageable);

    @Operation(summary = "내 활동 목록 조회", description = "로그인한 사용자의 파인더 탐색 세션 이력을 조회합니다. 마이페이지 '내 활동'에 사용합니다.")
    ApiResponse<Slice<FinderSessionListResponseDto>> getMyActivities(CustomOAuth2User user, Pageable pageable);

    @Operation(summary = "회원 탈퇴", description = "계정을 탈퇴 처리합니다. 작성한 게시글과 채팅 기록은 유지되며 개인정보는 제거됩니다. 같은 소셜 계정으로 재가입할 수 있습니다.")
    ApiResponse<Void> withdraw(CustomOAuth2User user);
}
