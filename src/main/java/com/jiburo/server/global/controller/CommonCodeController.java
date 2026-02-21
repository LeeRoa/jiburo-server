package com.jiburo.server.global.controller;

import com.jiburo.server.global.cache.CommonCodeCache;
import com.jiburo.server.global.dto.CommonCodeResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/common-codes")
public class CommonCodeController {

    private final CommonCodeCache commonCodeCache;

    /**
     * 전체 공통코드 조회 (Flutter 앱 시작 시 1회 호출 → 로컬 캐싱)
     *
     * GET /api/v1/common-codes
     *
     * 응답 예시:
     * {
     *   "ANIMAL": [{ "code": "DOG" }, { "code": "CAT" }],
     *   "STATUS": [{ "code": "LOST" }, { "code": "COMPLETE" }],
     *   "BADGE":  [{ "code": "BEGINNER", "ref1": "0" }, ...]
     * }
     */
    @GetMapping
    public ApiResponse<Map<String, List<CommonCodeResponseDto>>> getAllCodes() {
        Map<String, List<CommonCodeResponseDto>> result = commonCodeCache.getAll()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(CommonCodeResponseDto::from)
                                .toList()
                ));

        return ApiResponse.success(result);
    }

    /**
     * 특정 그룹의 코드 목록 조회 (드롭다운 등 개별 용도)
     *
     * GET /api/v1/common-codes/{group}
     */
    @GetMapping("/{group}")
    public ApiResponse<List<CommonCodeResponseDto>> getCodesByGroup(@PathVariable String group) {
        List<CommonCodeResponseDto> result = commonCodeCache.getGroup(group.toUpperCase())
                .stream()
                .map(CommonCodeResponseDto::from)
                .toList();

        return ApiResponse.success(result);
    }
}
