package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.LostPost;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LostPostRepository extends JpaRepository<LostPost, Long>, LostPostRepositoryCustom {

    // 삭제(Soft Delete)되지 않은 게시글 단건 조회 — 게시글 조회는 findById 대신 이 메서드를 사용할 것
    Optional<LostPost> findByIdAndDeletedFalse(Long id);

    /**
     * 위와 같지만 게시글 행에 쓰기 잠금(SELECT ... FOR UPDATE)을 건다 — 트랜잭션이 끝날 때까지 같은 게시글을 잠그려는
     * 다른 요청은 대기한다. 채팅방 생성처럼 "없으면 만든다"를 게시글 단위로 한 번에 하나씩 처리할 때 사용.
     * 트랜잭션의 첫 조회로 호출해야 이후 일반 조회가 잠금 이후 시점(다른 요청이 커밋한 결과)을 본다 (REPEATABLE READ).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM LostPost p WHERE p.id = :id AND p.deleted = false")
    Optional<LostPost> findByIdAndDeletedFalseForUpdate(@Param("id") Long id);
}
