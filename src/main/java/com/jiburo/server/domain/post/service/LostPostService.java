package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface LostPostService {

    Long create(UUID userId, LostPostCreateRequestDto requestDto);

    LostPostResponseDto findById(Long id, UUID userId);

    void update(UUID userId, Long postId, LostPostUpdateRequestDto requestDto);

    void updateStatus(UUID userId, Long postId, LostPostStatusUpdateRequestDto requestDto);

    void delete(UUID userId, Long postId);

    Page<LostPostResponseDto> search(LostPostSearchCondition condition, Pageable pageable, UUID userId);

    // viewerId: 비로그인이면 null (공개 범위 필터링용)
    List<LostPostResponseDto> getPostsForMap(LostPostMapRequestDto request, UUID viewerId);

    Slice<LostPostResponseDto> getPostsForList(LostPostNearbyRequestDto request, UUID viewerId);

    // 좋아요 토글 (좋아요면 true, 취소면 false 반환)
    boolean toggleLike(UUID userId, Long postId);

    // 내가 좋아요한 게시글 목록
    Page<LostPostResponseDto> getLikedPosts(UUID userId, Pageable pageable);
}
