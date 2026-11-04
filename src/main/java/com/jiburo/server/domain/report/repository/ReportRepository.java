package com.jiburo.server.domain.report.repository;

import com.jiburo.server.domain.report.domain.Report;
import com.jiburo.server.domain.report.domain.enums.ReportReason;
import com.jiburo.server.domain.report.domain.enums.ReportStatus;
import com.jiburo.server.domain.report.domain.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndTargetKeyAndStatus(UUID reporterId, String targetKey, ReportStatus status);

    @EntityGraph(attributePaths = {"reporter", "targetUser"})
    @Query(value = "select r from Report r"
            + " where (:status is null or r.status = :status)"
            + " and (:targetType is null or r.targetType = :targetType)"
            + " and (:reason is null or r.reason = :reason)"
            + " and (:targetUserId is null or r.targetUser.id = :targetUserId)",
            countQuery = "select count(r) from Report r"
                    + " where (:status is null or r.status = :status)"
                    + " and (:targetType is null or r.targetType = :targetType)"
                    + " and (:reason is null or r.reason = :reason)"
                    + " and (:targetUserId is null or r.targetUser.id = :targetUserId)")
    Page<Report> search(@Param("status") ReportStatus status,
                        @Param("targetType") ReportTargetType targetType,
                        @Param("reason") ReportReason reason,
                        @Param("targetUserId") UUID targetUserId,
                        Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "targetUser"})
    Optional<Report> findWithUsersById(Long id);

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findAllByTargetKeyAndStatus(String targetKey, ReportStatus status);

    // 목록에 "같은 대상 대기 중 신고 수"를 붙이기 위한 집계: [targetKey, count]
    @Query("select r.targetKey, count(r) from Report r where r.status = :status and r.targetKey in :keys group by r.targetKey")
    List<Object[]> countByTargetKeys(@Param("status") ReportStatus status, @Param("keys") Collection<String> keys);

    long countByTargetUser_Id(UUID targetUserId);

    long countByStatus(ReportStatus status);
}
