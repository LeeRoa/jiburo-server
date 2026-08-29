package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatMessageSearchCondition(
        @Schema(description = "검색 키워드 (메시지 내용 포함 검색)") String keyword,
        @Schema(description = "메시지 타입 코드로 필터링") ChatMsgType messageTypeCode,
        @Schema(description = "검색 시작일") String fromDate,
        @Schema(description = "검색 종료일") String toDate
) {}