package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.LostPostMapRequestDto;
import com.jiburo.server.domain.post.dto.LostPostNearbyRequestDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

/**
 * 모든 조회는 삭제되지 않은 게시글 중 viewerId(비로그인이면 null)가 볼 수 있는 게시글만 반환한다.
 */
public interface LostPostRepositoryCustom {
    Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable, UUID viewerId);

    // 뷰포트 검색 (사각형 범위)
    List<LostPost> searchByViewport(LostPostMapRequestDto request, UUID viewerId);

    // 반경 검색 (원형 범위 + 거리순 정렬)
    Slice<LostPost> searchByRadius(LostPostNearbyRequestDto request, UUID viewerId);
}
