package com.jiburo.server.global.setting;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

/**
 * 관리자가 기본값에서 바꾼 운영 설정값 (기본값 그대로인 설정은 행이 없다).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_settings")
public class AppSetting extends BaseTimeEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", length = 100)
    @Comment("설정 키 (SettingKey enum 이름)")
    private SettingKey settingKey;

    @Column(name = "setting_value", nullable = false, length = 1000)
    @Comment("설정 값 (문자열로 저장, SettingType으로 검증)")
    private String settingValue;

    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "updated_by")
    @Comment("마지막으로 수정한 관리자 ID")
    private UUID updatedBy;

    public AppSetting(SettingKey settingKey, String settingValue, UUID updatedBy) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedBy = updatedBy;
    }

    public void change(String settingValue, UUID updatedBy) {
        this.settingValue = settingValue;
        this.updatedBy = updatedBy;
    }
}
