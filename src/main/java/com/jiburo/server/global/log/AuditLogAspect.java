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
            Long userId = 0L; // 임시: 로그인 안 했으면 0 (Guest)

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User) {
                userId = ((CustomOAuth2User) auth.getPrincipal()).getUserId();
            }

            // 3. 파라미터 정보 (무엇을 수정했는지)
            Object[] args = joinPoint.getArgs();
            String targetData = (args.length > 0) ? args[0].toString() : "No Args";

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
}