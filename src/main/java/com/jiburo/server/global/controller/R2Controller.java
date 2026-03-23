package com.jiburo.server.global.controller;

import com.jiburo.server.global.dto.PresignedUrlRequestDto;
import com.jiburo.server.global.dto.PresignedUrlResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.service.R2Service;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
@Tag(name = "R2 Storage", description = "R2 스토리지 관련 API")
public class R2Controller {

    private final R2Service r2Service;

    @PostMapping
    public ApiResponse<PresignedUrlResponseDto> getPresignedUrl(
            @RequestBody PresignedUrlRequestDto request
    ) {
        return ApiResponse.success(r2Service.createPresignedUrl(request));
    }
}