package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminSettingResponseDto;
import com.jiburo.server.domain.admin.dto.AdminSettingUpdateRequestDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

public interface AdminSettingControllerDocs {

    @Operation(summary = "[관리자] 운영 설정 목록",
            description = "관리자 웹에서 변경 가능한 모든 운영 설정과 현재 값/기본값/허용 범위를 조회합니다.")
    ApiResponse<List<AdminSettingResponseDto>> getSettings();

    @Operation(summary = "[관리자] 운영 설정 단건 조회")
    ApiResponse<AdminSettingResponseDto> getSetting(@Parameter(description = "설정 키 (예: POST_MAX_IMAGES)") String key);

    @Operation(summary = "[관리자] 운영 설정 변경",
            description = "값을 변경하면 이 서버에는 즉시, 다른 서버에는 최대 1분 내에 반영됩니다. 타입/범위가 맞지 않으면 INVALID_SETTING_VALUE.")
    ApiResponse<AdminSettingResponseDto> updateSetting(@Parameter(hidden = true) CustomOAuth2User admin,
                                                       @Parameter(description = "설정 키") String key,
                                                       AdminSettingUpdateRequestDto requestDto);

    @Operation(summary = "[관리자] 운영 설정 기본값 복원", description = "관리자가 바꾼 값을 지우고 기본값으로 되돌립니다.")
    ApiResponse<AdminSettingResponseDto> resetSetting(@Parameter(description = "설정 키") String key);
}
