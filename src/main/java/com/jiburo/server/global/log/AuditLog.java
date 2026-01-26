package com.jiburo.server.global.log;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 누가 (User 객체 대신 ID만 저장, 연관관계 X)

    @Column(nullable = false)
    private String action; // 무엇을 (예: POST_CREATE)

    @Column(columnDefinition = "TEXT")
    private String targetData; // 상세 내용 (JSON 형태 저장 추천)

    private String clientIp; // IP 주소

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 언제

    @Builder
    public AuditLog(Long userId, String action, String targetData, String clientIp) {
        this.userId = userId;
        this.action = action;
        this.targetData = targetData;
        this.clientIp = clientIp;
    }
}