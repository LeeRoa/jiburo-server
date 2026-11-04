package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.moderation.domain.BannedWord;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdminBannedWordDtos {

    private AdminBannedWordDtos() {
    }

    public record WordResponse(
            @Schema(description = "금지어 ID (인코딩된 문자열)") String id,
            @Schema(description = "금지어 (소문자, 공백 제거된 저장 형태)") String word,
            @Schema(description = "등록한 관리자 ID") UUID createdBy,
            @Schema(description = "등록한 관리자 닉네임 (탈퇴 등으로 없으면 null)") String createdByNickname,
            @Schema(description = "등록 시각") LocalDateTime createdAt,
            @Schema(description = "마지막으로 수정한 관리자 ID (수정 전에는 등록자)") UUID updatedBy,
            @Schema(description = "마지막으로 수정한 관리자 닉네임") String updatedByNickname,
            @Schema(description = "마지막 수정 시각") LocalDateTime updatedAt
    ) {
        /** nicknames: 관리자 ID → 닉네임 */
        public static WordResponse of(BannedWord w, Map<UUID, String> nicknames) {
            return new WordResponse(HashidsUtils.encode(w.getId()), w.getWord(),
                    w.getCreatedBy(), nicknames.get(w.getCreatedBy()), w.getCreatedAt(),
                    w.getUpdatedBy(), nicknames.get(w.getUpdatedBy()), w.getUpdatedAt());
        }
    }

    public record AddRequest(
            @Schema(description = "추가할 금지어 목록 (대소문자·공백은 무시하고 저장)")
            @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 50) String> words
    ) {}

    public record UpdateRequest(
            @Schema(description = "바꿀 금지어 (대소문자·공백은 무시하고 저장)") @NotBlank @Size(max = 50) String word
    ) {}

    public record AddResult(
            @Schema(description = "새로 추가된 금지어") List<WordResponse> added,
            @Schema(description = "이미 등록돼 있어 건너뛴 금지어") List<String> skipped
    ) {}

    public record TestResult(
            @Schema(description = "가린 결과") String masked,
            @Schema(description = "금지어 포함 여부") boolean matched
    ) {}
}
