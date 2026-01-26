package com.jiburo.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class AsyncConfig {
    // TODO 여기서 스레드 풀(Thread Pool) 상세 설정 가능
}