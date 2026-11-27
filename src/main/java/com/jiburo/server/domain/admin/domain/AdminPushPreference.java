package com.jiburo.server.domain.admin.domain;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

/**
 * 운영자 푸시 수신 설정. 행이 없으면 모든 항목을 받는 것으로 본다 (기본 켜짐).
 * 실제 수신은 시스템 모니터링(MONITORING) 노출 권한이 있는 관리자만.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_push_preferences")
public class AdminPushPreference extends BaseTimeEntity {

    @Id
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "system_alert", nullable = false)
    @Comment("시스템 장애·복구 알림 (DB·Redis 연결 실패, 서버 오류 급증)")
    private boolean systemAlert = true;

    @Column(name = "storage_alert", nullable = false)
    @Comment("저장소 용량 경고 (80% / 95%)")
    private boolean storageAlert = true;

    public AdminPushPreference(UUID userId) {
        this.userId = userId;
    }

    public void change(Boolean systemAlert, Boolean storageAlert) {
        if (systemAlert != null) this.systemAlert = systemAlert;
        if (storageAlert != null) this.storageAlert = storageAlert;
    }
}
