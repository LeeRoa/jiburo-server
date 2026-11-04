package com.jiburo.server.domain.emoticon.dto;

import com.jiburo.server.domain.emoticon.domain.Emoticon;
import com.jiburo.server.domain.emoticon.domain.EmoticonPack;
import com.jiburo.server.global.util.HashidsUtils;
import com.jiburo.server.global.util.R2UrlProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Comparator;
import java.util.List;

/**
 * 채팅 이모티콘 목록 (사용자용). 노출 중인 묶음만, 정렬 순서대로.
 */
public record EmoticonPackResponseDto(
        @Schema(description = "묶음 ID (인코딩된 문자열)") String packId,
        @Schema(description = "묶음 이름") String name,
        @Schema(description = "대표 이미지 URL (묶음의 첫 이모티콘)") String thumbnailUrl,
        @Schema(description = "이모티콘 목록") List<Item> emoticons
) {
    public record Item(
            @Schema(description = "이모티콘 ID (인코딩된 문자열) — 채팅 전송 시 content에 담는다") String emoticonId,
            @Schema(description = "이미지 URL (png / 움직이는 gif·webp)") String imageUrl
    ) {
        static Item of(Emoticon e) {
            return new Item(HashidsUtils.encode(e.getId()), R2UrlProvider.buildUrl(e.getImageKey()));
        }
    }

    public static EmoticonPackResponseDto of(EmoticonPack pack) {
        List<Item> items = pack.getEmoticons().stream()
                .sorted(Comparator.comparingInt(Emoticon::getSortOrder).thenComparing(Emoticon::getId))
                .map(Item::of)
                .toList();
        return new EmoticonPackResponseDto(
                HashidsUtils.encode(pack.getId()),
                pack.getName(),
                items.isEmpty() ? null : items.get(0).imageUrl(),
                items);
    }
}
