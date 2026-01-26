package com.jiburo.server.global.log.repository;

import com.jiburo.server.global.log.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}