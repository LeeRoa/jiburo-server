package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.global.util.HashidsUtils;

import java.time.LocalDateTime;
import java.util.List;

public record FinderSessionResponseDto(
        String id,
        String postId,
        String postTitle,
        String statusCode,
        double startLatitude,
        double startLongitude,
        String resultCode,
        String note,
        LocalDateTime submittedAt,
        List<FinderMediaResponseDto> media,
        LocalDateTime createdAt
) {
    public static FinderSessionResponseDto from(FinderSession session) {
        return new FinderSessionResponseDto(
                HashidsUtils.encode(session.getId()),
                HashidsUtils.encode(session.getPost().getId()),
                session.getPost().getTitle(),
                session.getStatusCode().name(),
                session.getStartLatitude(),
                session.getStartLongitude(),
                session.getResultCode() != null ? session.getResultCode().name() : null,
                session.getNote(),
                session.getSubmittedAt(),
                session.getMedia().stream().map(FinderMediaResponseDto::from).toList(),
                session.getCreatedAt()
        );
    }
}
