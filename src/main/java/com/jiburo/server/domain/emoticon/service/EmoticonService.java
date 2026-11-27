package com.jiburo.server.domain.emoticon.service;

import com.jiburo.server.domain.emoticon.dto.EmoticonPackResponseDto;

import java.util.List;

public interface EmoticonService {

    /** 채팅 이모티콘 목록 (노출 중이고 이모티콘이 1개 이상인 묶음). 메모리 캐시. */
    List<EmoticonPackResponseDto> findCatalog();

    /** 채팅 전송용: 사용 가능한 이모티콘이면 이미지 키를 반환, 아니면 EMOTICON_NOT_FOUND */
    String resolveImageKey(Long emoticonId);

    /** 관리자 변경 후 호출 — 트랜잭션 안이면 커밋 뒤에 캐시를 비운다 */
    void invalidateCatalog();
}
