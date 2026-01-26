package com.jiburo.server.global.log.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogEvent {
    private Long userId;
    private String action;
    private String targetData;
    private String clientIp;
}