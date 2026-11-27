package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.dto.AdminEmoticonDtos.*;
import com.jiburo.server.domain.emoticon.domain.Emoticon;
import com.jiburo.server.domain.emoticon.domain.EmoticonPack;
import com.jiburo.server.domain.emoticon.repository.EmoticonPackRepository;
import com.jiburo.server.domain.emoticon.service.EmoticonService;
import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.image.dto.PresignedUrlRequestDto;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.domain.image.service.ImageStorageService;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 이모티콘 관리.
 * 업로드 흐름: presign(이 서비스) → R2 PUT → /api/v1/images/complete → addEmoticons(imageKeys)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminEmoticonService {

    private final EmoticonPackRepository emoticonPackRepository;
    private final ImageMetaRepository imageMetaRepository;
    private final ImageStorageService imageStorageService;
    private final EmoticonService emoticonService;

    public List<PackResponse> findAll() {
        return emoticonPackRepository.findAllWithEmoticons().stream().map(PackResponse::of).toList();
    }

    @Transactional
    public PackResponse createPack(CreatePackRequest request) {
        EmoticonPack pack = emoticonPackRepository.save(EmoticonPack.builder()
                .name(request.name().trim())
                .sortOrder(emoticonPackRepository.findMaxSortOrder() + 1)
                .visible(Boolean.TRUE.equals(request.visible()))
                .build());
        emoticonService.invalidateCatalog();
        return PackResponse.of(pack);
    }

    @Transactional
    public PackResponse updatePack(Long packId, UpdatePackRequest request) {
        EmoticonPack pack = findPack(packId);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
            }
            pack.rename(request.name().trim());
        }
        if (request.visible() != null) {
            pack.changeVisible(request.visible());
        }
        emoticonService.invalidateCatalog();
        return PackResponse.of(pack);
    }

    /** 묶음과 소속 이모티콘 삭제. 채팅 메시지는 이미지 키를 저장하므로 과거 메시지는 그대로 보인다. */
    @Transactional
    public void deletePack(Long packId) {
        emoticonPackRepository.delete(findPack(packId));
        emoticonService.invalidateCatalog();
    }

    @Transactional
    public void reorderPacks(List<String> packIds) {
        List<EmoticonPack> packs = emoticonPackRepository.findAll();
        applyOrder(packs, EmoticonPack::getId, packIds, EmoticonPack::changeSortOrder);
        emoticonService.invalidateCatalog();
    }

    public PresignedUrlResponseDto presign(UUID adminId, PresignRequest request) {
        return imageStorageService.createPresignedUrl(adminId, new PresignedUrlRequestDto(
                UploadTargetType.EMOTICON, request.originalFileName(), request.extension(), request.fileSize()));
    }

    @Transactional
    public PackResponse addEmoticons(Long packId, List<String> imageKeys) {
        EmoticonPack pack = findPack(packId);
        for (String imageKey : imageKeys) {
            ImageMeta meta = imageMetaRepository.findByFileKeyAndUseYnTrue(imageKey)
                    .filter(m -> m.getFileCode() == UploadTargetType.EMOTICON)
                    .filter(m -> m.getStatusCode() == ImageStatus.COMPLETED)
                    .orElseThrow(() -> new JiburoException(ErrorCode.INVALID_EMOTICON_IMAGE));
            pack.addEmoticon(meta.getFileKey());
        }
        emoticonPackRepository.flush(); // 응답에 새 이모티콘 ID가 필요
        emoticonService.invalidateCatalog();
        return PackResponse.of(pack);
    }

    @Transactional
    public void reorderEmoticons(Long packId, List<String> emoticonIds) {
        EmoticonPack pack = findPack(packId);
        applyOrder(pack.getEmoticons(), Emoticon::getId, emoticonIds, Emoticon::changeSortOrder);
        emoticonService.invalidateCatalog();
    }

    @Transactional
    public void deleteEmoticon(Long packId, Long emoticonId) {
        EmoticonPack pack = findPack(packId);
        Emoticon emoticon = pack.getEmoticons().stream()
                .filter(e -> e.getId().equals(emoticonId))
                .findFirst()
                .orElseThrow(() -> new JiburoException(ErrorCode.EMOTICON_NOT_FOUND));
        pack.removeEmoticon(emoticon);
        emoticonService.invalidateCatalog();
    }

    private EmoticonPack findPack(Long packId) {
        return emoticonPackRepository.findWithEmoticonsById(packId)
                .orElseThrow(() -> new JiburoException(ErrorCode.EMOTICON_PACK_NOT_FOUND));
    }

    // 요청 ID 목록이 현재 항목 전체와 정확히 일치할 때만 순서를 0부터 다시 매긴다 (중간에 추가/삭제된 경우 덮어쓰지 않도록)
    private static <T> void applyOrder(Collection<T> items, Function<T, Long> idOf, List<String> encodedIds,
                                       java.util.function.ObjIntConsumer<T> setOrder) {
        List<Long> ids = encodedIds.stream().map(HashidsUtils::decode).toList();
        Map<Long, T> byId = items.stream().collect(Collectors.toMap(idOf, Function.identity()));
        if (ids.size() != byId.size() || new HashSet<>(ids).size() != ids.size() || !byId.keySet().containsAll(ids)) {
            throw new JiburoException(ErrorCode.INVALID_EMOTICON_ORDER);
        }
        for (int i = 0; i < ids.size(); i++) {
            setOrder.accept(byId.get(ids.get(i)), i);
        }
    }
}
