package com.jiburo.server.global.log.event;

import com.jiburo.server.global.log.AuditLog;
import com.jiburo.server.global.log.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final AuditLogRepository auditLogRepository;

    @Async // 비동기로 실행 (메인 스레드 대기 X)
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도 트랜잭션으로 저장
    public void handleAuditLog(AuditLogEvent event) {
        log.info("감사 로그 저장 중... Action: {}", event.getAction());

        AuditLog logEntity = AuditLog.builder()
                .userId(event.getUserId())
                .action(event.getAction())
                .clientIp(event.getClientIp())
                .targetData(event.getTargetData())
                .build();

        auditLogRepository.save(logEntity);
    }
}