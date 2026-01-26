package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostPostRepository extends JpaRepository<LostPost, Long>, LostPostRepositoryCustom {

}