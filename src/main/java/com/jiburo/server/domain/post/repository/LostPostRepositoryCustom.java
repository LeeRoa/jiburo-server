package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.post.dto.LostPostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LostPostRepositoryCustom {
    Page<LostPost> search(LostPostSearchCondition condition, Pageable pageable);
}