package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;

public record ChatMessageSearchCondition(
        String keyword,
        ChatMsgType messageTypeCode,
        String fromDate,
        String toDate
) {}