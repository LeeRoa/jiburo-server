package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LostPostService {

    // 등록
    Long create(UUID userId, LostPostCreateRequestDto requestDto);

    // 단건 조회
    LostPostResponseDto findById(Long id);

    // 수정
    void update(UUID userId, Long postId, LostPostUpdateRequestDto requestDto);

    // 상태 변경
    void updateStatus(UUID userId, Long postId, String statusCode);

    // 삭제
    void delete(UUID userId, Long postId);

    Page<LostPostResponseDto> search(LostPostSearchCondition condition, Pageable pageable);

    List<LostPostResponseDto> getPostsForMap(LostPostMapRequestDto request);

    List<LostPostResponseDto> getPostsForList(LostPostNearbyRequestDto request);
}
