package com.jiburo.server.global.i18n;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

/**
 * 관리자가 관리자 웹에서 수정/추가한 다국어 문구. properties 파일의 같은 키보다 우선한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "i18n_messages", uniqueConstraints = @UniqueConstraint(
        name = "uq_i18n_message_key_locale", columnNames = {"message_key", "locale"}))
public class I18nMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false, length = 200)
    @Comment("메시지 키 (예: post.animal.dog, error.post.not_found)")
    private String messageKey;

    @Column(nullable = false, length = 20)
    @Comment("언어 태그 (BCP 47, 예: ko, en, ja, zh-CN)")
    private String locale;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("문구")
    private String message;

    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "updated_by")
    @Comment("마지막 수정 관리자 ID")
    private UUID updatedBy;

    public I18nMessage(String messageKey, String locale, String message, UUID updatedBy) {
        this.messageKey = messageKey;
        this.locale = locale;
        this.message = message;
        this.updatedBy = updatedBy;
    }

    public void change(String message, UUID updatedBy) {
        this.message = message;
        this.updatedBy = updatedBy;
    }
}
