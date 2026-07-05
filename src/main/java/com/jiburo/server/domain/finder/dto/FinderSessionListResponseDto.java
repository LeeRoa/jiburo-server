package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.global.util.HashidsUtils;

import java.time.LocalDateTime;

public record FinderSessionListResponseDto(
        String id,
        String postId,
        String postTitle,
        String postImageUrl,
        String statusCode,
        double startLatitude,
        double startLongitude,
        String resultCode,
        int mediaCount,
        LocalDateTime createdAt
) {
    public static FinderSessionListResponseDto from(FinderSession session) {
        return new FinderSessionListResponseDto(
                HashidsUtils.encode(session.getId()),
                HashidsUtils.encode(session.getPost().getId()),
                session.getPost().getTitle(),
                session.getPost().getImageUrl(),
                session.getStatusCode().name(),
                session.getStartLatitude(),
                session.getStartLongitude(),
                session.getResultCode() != null ? session.getResultCode().name() : null,
                session.getMedia().size(),
                session.getCreatedAt()
        );
    }
}
