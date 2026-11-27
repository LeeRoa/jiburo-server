package com.jiburo.server.domain.push.dto;

import com.jiburo.server.domain.push.domain.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushDeviceRequestDto(
        @Schema(description = "FCM 등록 토큰 (Flutter firebase_messaging getToken())") @NotBlank @Size(max = 500) String token,
        @Schema(description = "기기 종류 (ANDROID / IOS) — 해제 요청에서는 생략 가능") DevicePlatform platform
) {
    // 토큰 원문이 감사 로그에 남지 않도록
    @Override
    public String toString() {
        return "PushDeviceRequestDto[token=MASKED, platform=" + platform + "]";
    }
}
