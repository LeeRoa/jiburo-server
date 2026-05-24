package com.jiburo.server.global.log.event;

import com.jiburo.server.global.domain.enums.LogActionType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuditLogEvent {
    private UUID userId;
    private LogActionType action;
    private String targetData;
    private String clientIp;
}