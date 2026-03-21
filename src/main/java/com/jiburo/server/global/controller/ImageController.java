package com.jiburo.server.global.controller;

import com.jiburo.server.global.dto.PresignedUrlRequestDto;
import com.jiburo.server.global.dto.PresignedUrlResponseDto;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    public ApiResponse<PresignedUrlResponseDto> getPresignedUrl(@RequestBody PresignedUrlRequestDto request) {
        return ApiResponse.success(imageService.createPresignedUrl(request.extension()));
    }
}