package com.jiburo.server.domain.admin.dto;

import com.jiburo.server.domain.emoticon.domain.Emoticon;
import com.jiburo.server.domain.emoticon.domain.EmoticonPack;
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class AdminEmoticonDtos {

    private AdminEmoticonDtos() {
    }

    public record EmoticonItem(
            @Schema(description = "이모티콘 ID (인코딩된 문자열)") String id,
            @Schema(description = "R2 이미지 키") String imageKey,
            @Schema(description = "이미지 URL") String imageUrl,
            @Schema(description = "묶음 안 정렬 순서") int sortOrder
    ) {
        public static EmoticonItem of(Emoticon e) {
            return new EmoticonItem(HashidsUtils.encode(e.getId()), e.getImageKey(),
                    R2UrlProvider.buildUrl(e.getImageKey()), e.getSortOrder());
        }
    }

    public record PackResponse(
            @Schema(description = "묶음 ID (인코딩된 문자열)") String id,
            @Schema(description = "묶음 이름") String name,
            @Schema(description = "사용자 노출 여부") boolean visible,
            @Schema(description = "정렬 순서") int sortOrder,
            @Schema(description = "이모티콘 목록 (정렬 순서대로)") List<EmoticonItem> emoticons,
            @Schema(description = "생성 시각") LocalDateTime createdAt
    ) {
        public static PackResponse of(EmoticonPack p) {
            return new PackResponse(HashidsUtils.encode(p.getId()), p.getName(), p.isVisible(), p.getSortOrder(),
                    p.getEmoticons().stream()
                            .sorted(Comparator.comparingInt(Emoticon::getSortOrder).thenComparing(Emoticon::getId))
                            .map(EmoticonItem::of).toList(),
                    p.getCreatedAt());
        }
    }

    public record CreatePackRequest(
            @Schema(description = "묶음 이름") @NotBlank @Size(max = 50) String name,
            @Schema(description = "바로 노출할지 여부 (기본 false: 이모티콘을 다 올린 뒤 켜는 것을 권장)") Boolean visible
    ) {}

    public record UpdatePackRequest(
            @Schema(description = "묶음 이름 (null이면 유지)") @Size(min = 1, max = 50) String name,
            @Schema(description = "노출 여부 (null이면 유지)") Boolean visible
    ) {}

    public record OrderRequest(
            @Schema(description = "새 순서대로 나열한 ID 목록 (인코딩된 문자열). 현재 항목 전체와 정확히 일치해야 함")
            @NotEmpty @Size(max = 500) List<String> ids
    ) {}

    public record PresignRequest(
            @Schema(description = "파일 확장자 (점 제외: png / gif / webp)") @NotBlank String extension,
            @Schema(description = "파일 크기 (byte)") @NotNull @Positive Long fileSize,
            @Schema(description = "원본 파일명") String originalFileName
    ) {}

    public record AddEmoticonsRequest(
            @Schema(description = "업로드 완료(/api/v1/images/complete)된 이미지 키 목록. 순서대로 묶음 끝에 추가")
            @NotEmpty @Size(max = 100) List<@NotBlank String> imageKeys
    ) {}
}
