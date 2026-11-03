package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LostPostRepository extends JpaRepository<LostPost, Long>, LostPostRepositoryCustom {

    // 삭제(Soft Delete)되지 않은 게시글 단건 조회 — 게시글 조회는 findById 대신 이 메서드를 사용할 것
    Optional<LostPost> findByIdAndDeletedFalse(Long id);
}
