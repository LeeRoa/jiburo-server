package com.jiburo.server.global.repository;

import com.jiburo.server.global.domain.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {

    List<CommonCode> findAllByUseYnTrue();

    @Modifying
    @Query(value = """
            INSERT INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
            VALUES (:codeGroup, :code, :messageKey, :ref1, true, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                message_key = VALUES(message_key),
                ref_1       = VALUES(ref_1),
                use_yn      = true,
                updated_at  = NOW()
            """, nativeQuery = true)
    void upsert(
            @Param("codeGroup") String codeGroup,
            @Param("code") String code,
            @Param("messageKey") String messageKey,
            @Param("ref1") String ref1
    );
}
