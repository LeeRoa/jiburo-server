package com.jiburo.server.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 서버가 여러 대일 때 같은 장애 알림이 서버 수만큼 나가지 않도록 Redis로 "이미 보냄"을 공유한다.
 * Redis 자체가 죽었으면 막지 않고 보낸다 (알림 누락보다 중복이 낫다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertDeduplicator {

    private static final String PREFIX = "admin-alert:";
    private static final Duration TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    /** 장애 시작 알림을 이 서버가 보내야 하면 true */
    public boolean claimActivation(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(PREFIX + key, "1", TTL));
        } catch (RuntimeException e) {
            log.debug("알림 중복 확인 실패 — 그대로 발송: {}", e.getMessage());
            return true;
        }
    }

    /** 복구 알림을 이 서버가 보내야 하면 true (먼저 복구를 본 서버가 키를 지우고 보낸다) */
    public boolean claimRecovery(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(PREFIX + key));
        } catch (RuntimeException e) {
            log.debug("알림 중복 확인 실패 — 그대로 발송: {}", e.getMessage());
            return true;
        }
    }
}
