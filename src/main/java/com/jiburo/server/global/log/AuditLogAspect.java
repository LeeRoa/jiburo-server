package com.jiburo.server.global.log;

import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.log.event.AuditLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    // @AuditLog가 붙은 메소드가 '성공적으로' 끝났을 때 실행
    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void doAuditLog(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            // 1. 요청 정보 가져오기 (IP 등)
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String clientIp = request.getRemoteAddr();

            // 2. 사용자 ID 가져오기
            UUID userId = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User) {
                userId = ((CustomOAuth2User) auth.getPrincipal()).getUserId();
            }

            // audit_logs.user_id는 NOT NULL — 비로그인 요청은 저장할 수 없으므로 건너뛴다.
            // (로그인/재발급처럼 인증 전 시점의 로그는 서비스에서 userId를 채워 직접 발행)
            if (userId == null) {
                log.debug("감사 로그 생략 (인증 정보 없음): action={}", auditLog.action());
                return;
            }

            // 3. 파라미터 정보 (무엇을 수정했는지)
            //    로그인 사용자/요청 객체는 의미가 없으므로 제외하고 나머지 인자를 기록
            String targetData = describeArgs(joinPoint.getArgs());

            // 4. 이벤트 발행 (DB 저장은 여기서 안 함!)
            AuditLogEvent event = AuditLogEvent.builder()
                    .userId(userId)
                    .action(auditLog.action())
                    .clientIp(clientIp)
                    .targetData(targetData)
                    .build();

            eventPublisher.publishEvent(event);

        } catch (Exception e) {
            // 로그 남기려다 메인 로직이 죽으면 안 되므로 에러는 삼킴
            log.error("감사 로그 기록 중 오류 발생", e);
        }
    }

    private static final int MAX_TARGET_DATA_LENGTH = 2000;

    private String describeArgs(Object[] args) {
        String joined = java.util.Arrays.stream(args)
                .filter(java.util.Objects::nonNull)
                .filter(arg -> !(arg instanceof CustomOAuth2User))
                .filter(arg -> !(arg instanceof jakarta.servlet.ServletRequest))
                .filter(arg -> !(arg instanceof jakarta.servlet.ServletResponse))
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining(", "));
        if (joined.isEmpty()) {
            return "No Args";
        }
        return joined.length() > MAX_TARGET_DATA_LENGTH ? joined.substring(0, MAX_TARGET_DATA_LENGTH) + "..." : joined;
    }
}