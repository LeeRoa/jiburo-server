package com.jiburo.server.domain.finder.dto;

import com.jiburo.server.domain.finder.domain.enums.FinderResultType;
import jakarta.validation.constraints.NotNull;

public record FinderSessionSubmitRequestDto(
        @NotNull FinderResultType resultCode,
        String note
) {}
