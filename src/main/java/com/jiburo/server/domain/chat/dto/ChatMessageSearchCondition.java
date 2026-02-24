package com.jiburo.server.domain.chat.dto;

public record ChatMessageSearchCondition(
        String keyword,
        String messageTypeCode,
        String fromDate,
        String toDate
) {}
