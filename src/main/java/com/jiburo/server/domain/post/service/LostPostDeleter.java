package com.jiburo.server.domain.post.service;

import com.jiburo.server.domain.finder.repository.FinderSessionRepository;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 삭제(Soft Delete) 공통 처리 — 작성자 삭제와 관리자 강제 삭제가 같은 규칙을 쓰도록 한 곳에 둔다.
 * - 게시글: deleted 플래그 (채팅방/탐색 세션 기록은 보존, 목록·상세·지도에서 제외)
 * - 좋아요: 삭제 (복구해도 되살리지 않음)
 * - 진행 중(ACTIVE) 탐색 세션: 취소
 */
@Component
@RequiredArgsConstructor
public class LostPostDeleter {

    private final PostLikeRepository postLikeRepository;
    private final FinderSessionRepository finderSessionRepository;

    @Transactional
    public void delete(LostPost post) {
        post.softDelete();
        postLikeRepository.deleteAllByLostPostId(post.getId());
        finderSessionRepository.cancelActiveSessionsByPostId(post.getId());
    }
}
