package com.jiburo.server.domain.finder.repository;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FinderSessionRepository extends JpaRepository<FinderSession, Long> {

    boolean existsByPost_IdAndFinder_IdAndStatusCode(Long postId, UUID finderId, FinderSessionStatus statusCode);

    @Query("SELECT fs FROM FinderSession fs JOIN FETCH fs.post WHERE fs.finder.id = :finderId ORDER BY fs.createdAt DESC")
    Slice<FinderSession> findByFinderIdWithPost(@Param("finderId") UUID finderId, Pageable pageable);

    @Query("SELECT fs FROM FinderSession fs JOIN FETCH fs.finder WHERE fs.post.id = :postId ORDER BY fs.createdAt DESC")
    Slice<FinderSession> findByPostIdWithFinder(@Param("postId") Long postId, Pageable pageable);

    // 미디어가 하나도 없는 세션도 조회되도록 LEFT JOIN FETCH (inner join이면 FINDER_SESSION_NOT_FOUND 발생)
    @Query("SELECT fs FROM FinderSession fs LEFT JOIN FETCH fs.media WHERE fs.id = :sessionId")
    Optional<FinderSession> findByIdWithMedia(@Param("sessionId") Long sessionId);

    // 게시글 삭제 시 진행 중(ACTIVE) 탐색 세션 일괄 취소
    @Modifying
    @Query("UPDATE FinderSession fs SET fs.statusCode = com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus.CANCELLED "
            + "WHERE fs.post.id = :postId AND fs.statusCode = com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus.ACTIVE")
    int cancelActiveSessionsByPostId(@Param("postId") Long postId);
}
