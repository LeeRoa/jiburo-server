package com.jiburo.server.domain.emoticon.repository;

import com.jiburo.server.domain.emoticon.domain.EmoticonPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmoticonPackRepository extends JpaRepository<EmoticonPack, Long> {

    // 이모티콘까지 한 번에 로딩 (트랜잭션 밖 캐시에서도 지연 로딩이 일어나지 않도록)
    @Query("select distinct p from EmoticonPack p left join fetch p.emoticons order by p.sortOrder, p.id")
    List<EmoticonPack> findAllWithEmoticons();

    @Query("select distinct p from EmoticonPack p left join fetch p.emoticons where p.visible = true order by p.sortOrder, p.id")
    List<EmoticonPack> findVisibleWithEmoticons();

    @Query("select p from EmoticonPack p left join fetch p.emoticons where p.id = :id")
    Optional<EmoticonPack> findWithEmoticonsById(Long id);

    @Query("select coalesce(max(p.sortOrder), -1) from EmoticonPack p")
    int findMaxSortOrder();
}
