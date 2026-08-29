package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.enums.FinderResultType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FinderSessionSubmitRequestDto(
        @Schema(description = "탐색 결과 코드 (FOUND: 찾음, NOT_FOUND: 못 찾음)") @NotNull FinderResultType resultCode,
        @Schema(description = "탐색 결과 메모") String note
) {}
