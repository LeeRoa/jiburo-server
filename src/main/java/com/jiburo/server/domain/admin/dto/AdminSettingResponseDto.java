package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.global.setting.AppSetting;
import com.jiburo.server.global.setting.SettingKey;
import com.jiburo.server.global.setting.SettingType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminSettingResponseDto(
        @Schema(description = "설정 키", example = "POST_MAX_IMAGES") String key,
        @Schema(description = "분류 (UPLOAD/CHAT/SEARCH/AUTH)") SettingKey.Category category,
        @Schema(description = "값 타입 (INT/LONG/DOUBLE/BOOLEAN/STRING/STRING_LIST)") SettingType type,
        @Schema(description = "현재 적용 중인 값") String value,
        @Schema(description = "기본값") String defaultValue,
        @Schema(description = "관리자가 기본값에서 변경했는지 여부") boolean overridden,
        @Schema(description = "허용 최솟값 (숫자 타입만)") Double min,
        @Schema(description = "허용 최댓값 (숫자 타입만)") Double max,
        @Schema(description = "단위 (화면 표시용)") String unit,
        @Schema(description = "공개 설정 API로 클라이언트에 노출되는지 여부") boolean clientVisible,
        @Schema(description = "설명") String description,
        @Schema(description = "마지막 수정 시각 (기본값이면 null)") LocalDateTime updatedAt,
        @Schema(description = "마지막 수정 관리자 ID (기본값이면 null)") UUID updatedBy
) {
    public static AdminSettingResponseDto of(SettingKey key, String currentValue, AppSetting stored) {
        return new AdminSettingResponseDto(
                key.name(),
                key.getCategory(),
                key.getType(),
                currentValue,
                key.getDefaultValue(),
                stored != null,
                key.getMin(),
                key.getMax(),
                key.getUnit(),
                key.isClientVisible(),
                key.getDescription(),
                stored != null ? stored.getUpdatedAt() : null,
                stored != null ? stored.getUpdatedBy() : null
        );
    }
}
