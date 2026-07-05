package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.image.domain.UploadTargetType;

import java.util.List;

public record FinderMediaAddRequestDto(
        List<String> fileKeys,  // 파일 제보 (복수 허용, nullable)
        UploadTargetType mediaType,
        Double latitude,
        Double longitude,
        String memo             // 텍스트 메모 / 텍스트 제보 (nullable)
) {}
