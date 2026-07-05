package com.jiburo.server.domain.finder.repository;

import com.jiburo.server.domain.finder.domain.FinderSessionMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinderSessionMediaRepository extends JpaRepository<FinderSessionMedia, Long> {

    Optional<FinderSessionMedia> findByIdAndSession_Id(Long mediaId, Long sessionId);

    int countBySession_Id(Long sessionId);
}
