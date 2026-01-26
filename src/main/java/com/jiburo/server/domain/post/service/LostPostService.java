package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.post.dto.LostPostCreateRequestDto;
import com.jiburo.server.domain.post.dto.LostPostResponseDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import com.jiburo.server.domain.post.dto.LostPostUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LostPostService {

    // 등록
    Long create(Long userId, LostPostCreateRequestDto requestDto);

    // 단건 조회
    LostPostResponseDto findById(Long id);

    // 수정
    void update(Long userId, Long postId, LostPostUpdateRequestDto requestDto);

    // 상태 변경
    void updateStatus(Long userId, Long postId, String statusCode);

    // 삭제
    void delete(Long userId, Long postId);

    Page<LostPostResponseDto> search(LostPostSearchCondition condition, Pageable pageable);
}