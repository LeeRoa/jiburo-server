package com.jiburo.server.global.controller;

import com.jiburo.server.global.dto.CommonCodeResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Map;

public interface CommonCodeControllerDocs {

    @Operation(summary = "전체 공통코드 조회", description = "전체 공통코드 조회 (Flutter 앱 시작 시 1회 호출 → 로컬 캐싱)\n\n"
            + "응답 예시:\n"
            + "{\n"
            + "  \"ANIMAL\": [{ \"code\": \"DOG\" }, { \"code\": \"CAT\" }],\n"
            + "  \"STATUS\": [{ \"code\": \"LOST\" }, { \"code\": \"COMPLETE\" }],\n"
            + "  \"BADGE\":  [{ \"code\": \"BEGINNER\", \"ref1\": \"0\" }, ...]\n"
            + "}")
    ApiResponse<Map<String, List<CommonCodeResponseDto>>> getAllCodes();

    @Operation(summary = "특정 그룹의 코드 목록 조회", description = "특정 그룹의 코드 목록 조회 (드롭다운 등 개별 용도)")
    ApiResponse<List<CommonCodeResponseDto>> getCodesByGroup(
            @Parameter(description = "공통코드 그룹명 (예: CATEGORY, STATUS, GENDER 등)") String group);

    @Operation(summary = "공통코드 캐시 수동 리로드", description = "공통코드 캐시 수동 리로드 (DB에서 운영 설정값 수정 후 호출)")
    ApiResponse<Void> reload();
}
