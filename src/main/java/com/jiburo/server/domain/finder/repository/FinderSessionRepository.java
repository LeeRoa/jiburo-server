package com.jiburo.server.domain.finder.repository;

import com.jiburo.server.domain.finder.domain.FinderSession;
import com.jiburo.server.domain.finder.domain.enums.FinderSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT fs FROM FinderSession fs JOIN FETCH fs.media WHERE fs.id = :sessionId")
    Optional<FinderSession> findByIdWithMedia(@Param("sessionId") Long sessionId);
}
