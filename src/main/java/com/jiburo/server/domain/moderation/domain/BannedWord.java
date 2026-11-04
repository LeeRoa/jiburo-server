package com.jiburo.server.domain.moderation.domain;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

/**
 * 금지어 (관리자 웹에서 관리). 채팅 텍스트·게시글 제목/내용에서는 *** 로 가려지고,
 * 닉네임에는 들어갈 수 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "banned_words", uniqueConstraints = @UniqueConstraint(name = "uq_banned_word", columnNames = "word"))
public class BannedWord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @Comment("금지어 (소문자, 공백 제거 후 저장)")
    private String word;

    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "created_by")
    @Comment("등록한 관리자 ID")
    private UUID createdBy;

    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "updated_by")
    @Comment("마지막으로 수정한 관리자 ID (등록 시에는 등록자)")
    private UUID updatedBy;

    public BannedWord(String word, UUID createdBy) {
        this.word = word;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public void change(String word, UUID adminId) {
        this.word = word;
        this.updatedBy = adminId;
    }
}
