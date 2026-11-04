package com.jiburo.server.domain.image.controller;

import com.jiburo.server.domain.image.dto.ImageUploadCompleteRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.image.service.ImageStorageService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
public class ImageController implements ImageControllerDocs {
    private final ImageStorageService imageStorageService;

    @Override
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponseDto> getPresignedUrl(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody PresignedUrlRequestDto request) {
        // 이모티콘 등 관리자 전용 대상은 관리자 API(/api/v1/admin/emoticons/presigned-url)로만 발급
        if (request.fileCode().isAdminOnly()) {
            throw new JiburoException(ErrorCode.INVALID_FILE_CODE);
        }
        return ApiResponse.success(imageStorageService.createPresignedUrl(user.getUserId(), request));
    }

    @Override
    @PostMapping("/complete")
    public ApiResponse<Void> completeUpload(
            @AuthenticationPrincipal CustomOAuth2User user,
            @Valid @RequestBody ImageUploadCompleteRequestDto request) {
        imageStorageService.completeUpload(user.getUserId(), request.fileKey());
        return ApiResponse.success();
    }
}
