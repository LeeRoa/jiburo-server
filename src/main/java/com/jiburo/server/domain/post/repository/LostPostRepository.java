package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LostPostRepository extends JpaRepository<LostPost, Long> {

    // N+1 문제 방지를 위한 Fetch Join
    @Query("SELECT p FROM LostPost p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    List<LostPost> findAllDesc();
}