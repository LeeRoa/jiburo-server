package com.jiburo.server.domain.emoticon.service;

import com.jiburo.server.domain.emoticon.domain.Emoticon;
import com.jiburo.server.domain.emoticon.dto.EmoticonPackResponseDto;
import com.jiburo.server.domain.emoticon.repository.EmoticonPackRepository;
import com.jiburo.server.domain.emoticon.repository.EmoticonRepository;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * - 이모티콘 목록은 자주 읽고 거의 안 바뀌므로 메모리에 캐시한다.
 *   관리자 변경 시 invalidate, 스케일 아웃 시 다른 서버는 주기적 비우기(기본 60초)로 반영.
 */
@Service
@RequiredArgsConstructor
public class EmoticonServiceImpl implements EmoticonService {

    private final EmoticonPackRepository emoticonPackRepository;
    private final EmoticonRepository emoticonRepository;

    private volatile List<EmoticonPackResponseDto> catalog;

    @Override
    public List<EmoticonPackResponseDto> findCatalog() {
        List<EmoticonPackResponseDto> cached = catalog;
        if (cached == null) {
            // fetch join으로 이모티콘까지 로딩되므로 트랜잭션 없이 매핑해도 지연 로딩이 일어나지 않는다
            cached = emoticonPackRepository.findVisibleWithEmoticons().stream()
                    .filter(p -> !p.getEmoticons().isEmpty())
                    .map(EmoticonPackResponseDto::of)
                    .toList();
            catalog = cached;
        }
        return cached;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveImageKey(Long emoticonId) {
        Emoticon emoticon = emoticonRepository.findWithPackById(emoticonId)
                .filter(e -> e.getPack().isVisible())
                .orElseThrow(() -> new JiburoException(ErrorCode.EMOTICON_NOT_FOUND));
        return emoticon.getImageKey();
    }

    @Override
    public void invalidateCatalog() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 커밋 전에 비우면 동시 요청이 변경 전 데이터로 캐시를 다시 채울 수 있다
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    catalog = null;
                }
            });
        } else {
            catalog = null;
        }
    }

    @Scheduled(fixedDelayString = "${app.emoticon.catalog-cache-ttl-ms:60000}")
    public void evictPeriodically() {
        catalog = null;
    }
}
