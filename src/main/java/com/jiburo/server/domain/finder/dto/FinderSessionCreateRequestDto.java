package com.jiburo.server.domain.finder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinderSessionCreateRequestDto(
        @Schema(description = "탐색 대상 게시글 ID (인코딩된 문자열)") @NotBlank String postId,
        @Schema(description = "탐색 시작 위치 위도") @NotNull Double latitude,
        @Schema(description = "탐색 시작 위치 경도") @NotNull Double longitude
) {}
