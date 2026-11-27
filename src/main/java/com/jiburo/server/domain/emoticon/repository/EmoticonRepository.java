package com.jiburo.server.domain.emoticon.repository;

import com.jiburo.server.domain.emoticon.domain.Emoticon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmoticonRepository extends JpaRepository<Emoticon, Long> {

    @Query("select e from Emoticon e join fetch e.pack where e.id = :id")
    Optional<Emoticon> findWithPackById(Long id);
}
