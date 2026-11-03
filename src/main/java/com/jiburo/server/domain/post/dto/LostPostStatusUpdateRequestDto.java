package com.jiburo.server.domain.post.dto;

import com.jiburo.server.domain.post.domain.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LostPostStatusUpdateRequestDto(
        // enum 타입이라 허용 값(LOST|PROTECTING|COMPLETE) 검증은 Jackson 역직렬화가 담당한다.
        // (@Pattern은 문자열 전용이라 enum에 붙이면 검증 시 UnexpectedTypeException -> 500 발생)
        @Schema(description = "게시글 상태")
        @NotNull
        PostStatus statusCode,

        @Schema(description = "찾아준 사람 ID (우리 앱 회원인 경우만)") UUID finderId, // 해결사 ID (옵션 - 우리 앱 회원인 경우만 보냄)

        @Schema(description = "해결 메모", example = "경비실에서 보관해주심") String resultNote // 해결 메모 (옵션 - "경비실에서 보관해주심" 등)
) {
}
