package com.jiburo.server.domain.image.service;

import com.jiburo.server.domain.image.domain.ImageMeta;
import com.jiburo.server.domain.image.domain.ImageStatus;
import com.jiburo.server.domain.image.domain.UploadTargetType;
import com.jiburo.server.domain.image.repository.ImageMetaRepository;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 게시글·채팅·제보에 첨부되는 파일 키 검증.
 * 클라이언트가 보낸 문자열을 그대로 저장하면 외부 URL(http...)이나 남의 파일 키를 붙일 수 있으므로,
 * "본인이 presigned URL로 올려 업로드 완료(COMPLETED)한, 지정 용도의 파일"만 허용한다.
 */
@Component
@RequiredArgsConstructor
public class UploadedFileValidator {

    private final ImageMetaRepository imageMetaRepository;

    public void requireOwnedUpload(UUID ownerId, String fileKey, Set<UploadTargetType> allowedTypes) {
        requireOwnedUploads(ownerId, List.of(fileKey == null ? "" : fileKey), allowedTypes);
    }

    /** 모든 키가 유효해야 통과 (하나라도 아니면 INVALID_UPLOADED_FILE) */
    public void requireOwnedUploads(UUID ownerId, Collection<String> fileKeys, Set<UploadTargetType> allowedTypes) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }
        if (fileKeys.stream().anyMatch(k -> k == null || k.isBlank())) {
            throw new JiburoException(ErrorCode.INVALID_UPLOADED_FILE);
        }
        Set<String> distinct = new HashSet<>(fileKeys);
        Map<String, ImageMeta> found = imageMetaRepository.findAllByFileKeyInAndUseYnTrue(distinct).stream()
                .collect(Collectors.toMap(ImageMeta::getFileKey, Function.identity(), (a, b) -> a));

        for (String key : distinct) {
            ImageMeta meta = found.get(key);
            if (meta == null
                    || !meta.getUser().getId().equals(ownerId)
                    || meta.getStatusCode() != ImageStatus.COMPLETED
                    || !allowedTypes.contains(meta.getFileCode())) {
                throw new JiburoException(ErrorCode.INVALID_UPLOADED_FILE);
            }
        }
    }
}
