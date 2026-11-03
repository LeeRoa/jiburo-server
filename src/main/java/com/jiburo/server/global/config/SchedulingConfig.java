package com.jiburo.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 작업 활성화 (예: AppSettingService 운영 설정 주기적 새로고침)
 */
@EnableScheduling
@Configuration
public class SchedulingConfig {
}
