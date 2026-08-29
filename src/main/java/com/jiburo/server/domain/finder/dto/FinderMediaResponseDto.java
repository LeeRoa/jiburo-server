package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.FinderSessionMedia;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record FinderMediaResponseDto(
        @Schema(description = "제보 미디어 ID (인코딩된 문자열)") String id,
        @Schema(description = "제보 사진 파일 키") String fileKey,
        @Schema(description = "제보 미디어 업로드 대상 타입") String mediaType,
        @Schema(description = "제보 위치 위도") Double latitude,
        @Schema(description = "제보 위치 경도") Double longitude,
        @Schema(description = "텍스트 메모 / 텍스트 제보") String memo,
        @Schema(description = "제보 미디어 등록 시각") LocalDateTime createdAt
) {
    public static FinderMediaResponseDto from(FinderSessionMedia media) {
        return new FinderMediaResponseDto(
                HashidsUtils.encode(media.getId()),
                media.getFileKey(),
                media.getMediaType() != null ? media.getMediaType().name() : null,
                media.getLatitude(),
                media.getLongitude(),
                media.getMemo(),
                media.getCreatedAt()
        );
    }
}
