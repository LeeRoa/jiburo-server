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

public interface LostPostRepositoryCustom {
    // writerId가 있으면 해당 작성자의 게시글로 한정 (마이페이지 '내 게시물')
    Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable, UUID writerId);

    // 뷰포트 검색 (사각형 범위)
    List<LostPost> searchByViewport(LostPostMapRequestDto request);

    // 반경 검색 (원형 범위 + 거리순 정렬)
    Slice<LostPost> searchByRadius(LostPostNearbyRequestDto request);
}
