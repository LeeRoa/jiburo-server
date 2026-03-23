package com.jiburo.server.domain.image.controller;

import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.image.service.ImageStorageService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Image", description = "이미지 스토리지 작업 관련 API")
public class ImageController {
    private final ImageStorageService imageStorageService;

    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponseDto> getPresignedUrl(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody PresignedUrlRequestDto request) {
        return ApiResponse.success(imageStorageService.createPresignedUrl(user.getUserId(), request));
    }
}
