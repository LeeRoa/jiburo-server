package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.LostPostMapRequestDto;
import com.jiburo.server.domain.post.dto.LostPostNearbyRequestDto;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LostPostRepositoryCustom {
    Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable);

    // 뷰포트 검색 (사각형 범위)
    List<LostPost> searchByViewport(LostPostMapRequestDto request);

    // 반경 검색 (원형 범위 + 거리순 정렬)
    List<LostPost> searchByRadius(LostPostNearbyRequestDto request);
}
