package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.global.i18n.MessageCatalog.Source;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminI18nDtos {

    private AdminI18nDtos() {
    }

    public enum Status {
        @Schema(description = "전체") ALL,
        @Schema(description = "조회 언어 중 하나라도 문구가 없는 키 (번역 누락)") MISSING,
        @Schema(description = "관리자가 수정/추가한 문구가 있는 키") OVERRIDDEN
    }

    public record SearchCondition(
            @Schema(description = "키 또는 문구 부분 일치 (대소문자 무시)") String keyword,
            @Schema(description = "키 접두사 (예: error., common.)") String prefix,
            @Schema(description = "필터 (기본 ALL)") Status status
    ) {}

    public record LocaleValue(
            @Schema(description = "언어 태그") String locale,
            @Schema(description = "현재 적용 중인 문구 (대체 없이 이 언어에 등록된 값, 없으면 null)") String value,
            @Schema(description = "값 출처: DB(관리자 수정) / FILE(properties 기본값) / NONE(없음)") Source source,
            @Schema(description = "properties 기본값 (되돌리기 시 적용될 값, 없으면 null)") String defaultValue,
            @Schema(description = "관리자 수정 시각 (DB 값일 때만)") LocalDateTime updatedAt,
            @Schema(description = "수정한 관리자 ID (DB 값일 때만)") UUID updatedBy
    ) {}

    public record MessageRow(
            @Schema(description = "문구 키") String key,
            @Schema(description = "언어별 값 (locales 순서)") List<LocaleValue> values
    ) {}

    public record UpdateRequest(
            @Schema(description = "문구. {0}, {1} 같은 인자를 쓸 수 있고, 작은따옴표는 두 번('') 써야 한 번 표시됩니다.")
            @NotBlank @Size(max = 5000) String message
    ) {}
}
