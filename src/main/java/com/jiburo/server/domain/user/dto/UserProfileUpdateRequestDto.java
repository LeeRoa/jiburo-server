package com.jiburo.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequestDto(
        @Schema(description = "변경할 닉네임 (null이면 유지)")
        @Size(min = 2, max = 20, message = "{validation.user.nickname.size}")
        String nickname,

        @Schema(description = "변경할 프로필 이미지 URL (null이면 유지)")
        String profileImageUrl
) {}
