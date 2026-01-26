package com.jiburo.server.global.consts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate // 생성 시 날짜 자동 저장
    @Column(updatable = false) // 수정 시에는 관여하지 않음
    private LocalDateTime createdAt;

    @LastModifiedDate // 수정 시 날짜 자동 갱신
    private LocalDateTime updatedAt;
}