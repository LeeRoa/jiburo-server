package com.jiburo.server.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * 알림 생성에 필요한 정보를 담은 요청 객체
 */
public record NotificationCreateRequestDto(
        @Schema(description = "알림 수신자 ID")
        UUID receiverId,

        @Schema(description = "알림 발신자 ID")
        UUID senderId,

        @Schema(description = "알림 유형 (번역 키)")
        String typeCode,

        @Schema(description = "메시지 치환 인자 리스트 (저장 시 콤마로 합쳐짐)")
        List<String> args, // 생성 시점에 리스트로 받아서 저장할 때 콤마로 합침

        @Schema(description = "이동 대상 ID")
        Long targetId
) {
    /**
     * 리스트로 받은 인자들을 저장용 문자열(콤마 구분)로 변환
     */
    public String getArgsAsString() {
        return args != null ? String.join(",", args) : "";
    }
}
