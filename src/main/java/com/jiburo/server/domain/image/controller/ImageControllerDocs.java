package com.jiburo.server.domain.image.controller;

import com.jiburo.server.domain.image.dto.ImageUploadCompleteRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;

public interface ImageControllerDocs {

    @Operation(summary = "Presigned URL 발급", description = "Cloudflare R2 업로드용 presigned PUT URL을 발급받고, PENDING 상태의 ImageMeta를 생성합니다. 클라이언트는 응답의 presignedUrl로 R2에 직접 PUT 업로드(Content-Type 헤더는 응답의 contentType 그대로 — 서명에 포함되어 다르면 R2가 거절)한 뒤, fileKey를 완료 콜백(/complete)에 전달해야 합니다.")
    ApiResponse<PresignedUrlResponseDto> getPresignedUrl(CustomOAuth2User user, PresignedUrlRequestDto request);

    @Operation(summary = "업로드 완료 콜백", description = "클라이언트가 presigned URL로 R2 업로드를 마친 뒤 호출합니다. fileKey에 해당하는 ImageMeta 상태를 COMPLETED로 전환합니다. 이미 COMPLETED인 경우 idempotent하게 종료됩니다.")
    ApiResponse<Void> completeUpload(CustomOAuth2User user, ImageUploadCompleteRequestDto request);
}
