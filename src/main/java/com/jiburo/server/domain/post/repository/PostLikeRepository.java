package com.jiburo.server.domain.post.repository;

import com.jiburo.server.domain.post.domain.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUser_IdAndLostPost_Id(UUID userId, Long lostPostId);

    boolean existsByUser_IdAndLostPost_Id(UUID userId, Long lostPostId);

    long countByLostPost_Id(Long lostPostId);

    // 여러 게시글의 좋아요 수를 한 번에 조회 (N+1 방지)
    @Query("SELECT pl.lostPost.id, COUNT(pl) FROM PostLike pl WHERE pl.lostPost.id IN :postIds GROUP BY pl.lostPost.id")
    List<Object[]> countGroupByLostPostIdIn(@Param("postIds") List<Long> postIds);

    // 현재 사용자가 좋아요한 게시글 ID 목록을 한 번에 조회 (N+1 방지)
    @Query("SELECT pl.lostPost.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.lostPost.id IN :postIds")
    Set<Long> findLikedPostIdsByUser_IdAndPostIds(@Param("userId") UUID userId, @Param("postIds") List<Long> postIds);

    // 관심 게시글 목록 (ManyToOne fetch join은 페이징과 함께 사용 가능)
    // 삭제된 글, 좋아요 이후 작성자가 비공개(PRIVATE)로 바꾼 남의 글은 제외
    @Query(
            value = "SELECT pl FROM PostLike pl JOIN FETCH pl.lostPost lp JOIN FETCH lp.user "
                    + "WHERE pl.user.id = :userId AND lp.deleted = false "
                    + "AND (lp.visibilityCode <> com.jiburo.server.domain.post.domain.enums.VisibilityType.PRIVATE OR lp.user.id = :userId)",
            countQuery = "SELECT COUNT(pl) FROM PostLike pl JOIN pl.lostPost lp "
                    + "WHERE pl.user.id = :userId AND lp.deleted = false "
                    + "AND (lp.visibilityCode <> com.jiburo.server.domain.post.domain.enums.VisibilityType.PRIVATE OR lp.user.id = :userId)"
    )
    Page<PostLike> findByUser_IdWithPost(@Param("userId") UUID userId, Pageable pageable);

    // 게시글 삭제 시 좋아요 일괄 삭제
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.lostPost.id = :postId")
    int deleteAllByLostPostId(@Param("postId") Long postId);
}
