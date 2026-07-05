package com.jiburo.server.domain.finder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FinderSessionCreateRequestDto(
        @NotBlank String postId,
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
