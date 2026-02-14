package com.jiburo.server.domain.post.dto;

import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record LostPostStatusUpdateRequestDto(
        @Pattern(regexp = "LOST|PROTECTING|COMPLETE", message = "유효하지 않은 상태 코드입니다.")
        String statusCode,

        // 해결사 ID (옵션 - 우리 앱 회원인 경우만 보냄)
        UUID finderId,

        // 해결 메모 (옵션 - "경비실에서 보관해주심" 등)
        String resultNote
) {
}
