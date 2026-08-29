package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.image.domain.UploadTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FinderMediaAddRequestDto(
        @Schema(description = "제보 사진 파일 키 목록 (복수 허용, nullable)") List<String> fileKeys,  // 파일 제보 (복수 허용, nullable)
        @Schema(description = "제보 미디어 업로드 대상 타입") UploadTargetType mediaType,
        @Schema(description = "제보 위치 위도") Double latitude,
        @Schema(description = "제보 위치 경도") Double longitude,
        @Schema(description = "텍스트 메모 / 텍스트 제보 (nullable)") String memo             // 텍스트 메모 / 텍스트 제보 (nullable)
) {}
