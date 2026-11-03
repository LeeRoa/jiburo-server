package com.jiburo.server.global.controller;

import com.jiburo.server.global.cache.CommonCodeCache;
import com.jiburo.server.global.dto.CommonCodeResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/common-codes")
@Tag(name = "Common-Code", description = "공통 코드 관련 API")
public class CommonCodeController implements CommonCodeControllerDocs {

    private final CommonCodeCache commonCodeCache;

    @Override
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

    @Override
    @GetMapping("/{group}")
    public ApiResponse<List<CommonCodeResponseDto>> getCodesByGroup(@PathVariable String group) {
        List<CommonCodeResponseDto> result = commonCodeCache.getGroup(group.toUpperCase())
                .stream()
                .map(CommonCodeResponseDto::from)
                .toList();

        return ApiResponse.success(result);
    }

    // 캐시 리로드는 관리자 전용 API로 이동: POST /api/v1/admin/common-codes/reload (AdminCommonCodeController)
}
