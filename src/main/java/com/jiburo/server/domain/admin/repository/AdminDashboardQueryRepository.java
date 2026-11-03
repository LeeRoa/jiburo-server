package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.domain.post.domain.enums.PostStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 집계 쿼리. 기간은 [from, to) 반열린 구간.
 */
@Repository
@RequiredArgsConstructor
public class AdminDashboardQueryRepository {

    private final EntityManager em;

    public long countNewUsers(LocalDateTime from, LocalDateTime to) {
        return count("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :from AND u.createdAt < :to", from, to);
    }

    // 기간 내 등록된 게시글 (이후 삭제된 글 포함 — "얼마나 올라왔나" 지표)
    public long countNewPosts(LocalDateTime from, LocalDateTime to) {
        return count("SELECT COUNT(p) FROM LostPost p WHERE p.createdAt >= :from AND p.createdAt < :to", from, to);
    }

    /**
     * 기간 내 해결 완료된 게시글. 완료 시각 컬럼이 따로 없어 "COMPLETE 상태 + 마지막 수정 시각"으로 근사한다.
     * (완료 후 다시 수정되면 수정 시점으로 집계될 수 있음)
     */
    public long countCompletedPosts(LocalDateTime from, LocalDateTime to) {
        return em.createQuery("SELECT COUNT(p) FROM LostPost p WHERE p.statusCode = :status AND p.deleted = false "
                        + "AND p.updatedAt >= :from AND p.updatedAt < :to", Long.class)
                .setParameter("status", PostStatus.COMPLETE)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countNewChatRooms(LocalDateTime from, LocalDateTime to) {
        return count("SELECT COUNT(r) FROM ChatRoom r WHERE r.createdAt >= :from AND r.createdAt < :to", from, to);
    }

    public Map<LocalDate, Long> dailyNewUsers(LocalDateTime from, LocalDateTime to) {
        return daily("SELECT CAST(u.createdAt AS LocalDate), COUNT(u) FROM User u "
                + "WHERE u.createdAt >= :from AND u.createdAt < :to GROUP BY CAST(u.createdAt AS LocalDate)", from, to);
    }

    public Map<LocalDate, Long> dailyNewPosts(LocalDateTime from, LocalDateTime to) {
        return daily("SELECT CAST(p.createdAt AS LocalDate), COUNT(p) FROM LostPost p "
                + "WHERE p.createdAt >= :from AND p.createdAt < :to GROUP BY CAST(p.createdAt AS LocalDate)", from, to);
    }

    private long count(String jpql, LocalDateTime from, LocalDateTime to) {
        return em.createQuery(jpql, Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    private Map<LocalDate, Long> daily(String jpql, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        Map<LocalDate, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((LocalDate) row[0], (Long) row[1]);
        }
        return result;
    }
}
