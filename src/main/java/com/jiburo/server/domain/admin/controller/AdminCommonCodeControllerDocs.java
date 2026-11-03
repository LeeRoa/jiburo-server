package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;

public interface AdminCommonCodeControllerDocs {

    @Operation(summary = "[관리자] 공통코드 캐시 리로드",
            description = "DB의 common_codes를 다시 읽어 서버 메모리 캐시를 갱신합니다. ADMIN 권한 필요.")
    ApiResponse<Void> reload();
}
