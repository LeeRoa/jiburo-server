package com.jiburo.server.global.log.annotation;

import com.jiburo.server.global.domain.enums.LogActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    LogActionType action(); // 행동 이름 (필수)
}