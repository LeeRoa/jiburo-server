package com.jiburo.server.domain.notification.dto;

import java.util.List;
import java.util.UUID;

/**
 * 알림 생성에 필요한 정보를 담은 요청 객체
 */
public record NotificationCreateRequestDto(
        UUID receiverId,
        UUID senderId,
        String typeCode,
        List<String> args, // 생성 시점에 리스트로 받아서 저장할 때 콤마로 합침
        Long targetId
) {
    /**
     * 리스트로 받은 인자들을 저장용 문자열(콤마 구분)로 변환
     */
    public String getArgsAsString() {
        return args != null ? String.join(",", args) : "";
    }
}
