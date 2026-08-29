package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record FinderSessionListResponseDto(
        @Schema(description = "탐색 세션 ID (인코딩된 문자열)") String id,
        @Schema(description = "탐색 대상 게시글 ID (인코딩된 문자열)") String postId,
        @Schema(description = "탐색 대상 게시글 제목") String postTitle,
        @Schema(description = "탐색 대상 게시글 대표 이미지 URL") String postImageUrl,
        @Schema(description = "탐색 세션 상태 코드 (ACTIVE, SUBMITTED, CANCELLED)") String statusCode,
        @Schema(description = "탐색 시작 위치 위도") double startLatitude,
        @Schema(description = "탐색 시작 위치 경도") double startLongitude,
        @Schema(description = "탐색 결과 코드 (FOUND, NOT_FOUND, 미제출 시 null)") String resultCode,
        @Schema(description = "등록된 제보 미디어 개수") int mediaCount,
        @Schema(description = "탐색 세션 생성 시각") LocalDateTime createdAt
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
