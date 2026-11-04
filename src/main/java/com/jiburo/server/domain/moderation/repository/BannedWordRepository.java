package com.jiburo.server.domain.moderation.repository;

import com.jiburo.server.domain.moderation.domain.BannedWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {

    Page<BannedWord> findByWordContaining(String keyword, Pageable pageable);

    List<BannedWord> findByWordIn(Collection<String> words);
}
