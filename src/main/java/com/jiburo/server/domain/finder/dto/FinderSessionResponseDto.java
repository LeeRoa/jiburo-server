package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record FinderSessionResponseDto(
        @Schema(description = "탐색 세션 ID (인코딩된 문자열)") String id,
        @Schema(description = "탐색 대상 게시글 ID (인코딩된 문자열)") String postId,
        @Schema(description = "탐색 대상 게시글 제목") String postTitle,
        @Schema(description = "탐색 세션 상태 코드 (ACTIVE, SUBMITTED, CANCELLED)") String statusCode,
        @Schema(description = "탐색 시작 위치 위도") double startLatitude,
        @Schema(description = "탐색 시작 위치 경도") double startLongitude,
        @Schema(description = "탐색 결과 코드 (FOUND, NOT_FOUND, 미제출 시 null)") String resultCode,
        @Schema(description = "탐색 결과 메모") String note,
        @Schema(description = "탐색 결과 제출 시각") LocalDateTime submittedAt,
        @Schema(description = "탐색 중 등록된 제보 미디어 목록") List<FinderMediaResponseDto> media,
        @Schema(description = "탐색 세션 생성 시각") LocalDateTime createdAt
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
