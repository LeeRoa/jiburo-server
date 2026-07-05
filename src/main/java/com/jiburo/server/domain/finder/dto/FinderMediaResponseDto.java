package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.FinderSessionMedia;
import com.jiburo.server.global.util.HashidsUtils;

import java.time.LocalDateTime;

public record FinderMediaResponseDto(
        String id,
        String fileKey,
        String mediaType,
        Double latitude,
        Double longitude,
        String memo,
        LocalDateTime createdAt
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
