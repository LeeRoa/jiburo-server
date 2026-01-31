package com.jiburo.server.global.log.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuditLogEvent {
    private UUID userId;
    private String action;
    private String targetData;
    private String clientIp;
}