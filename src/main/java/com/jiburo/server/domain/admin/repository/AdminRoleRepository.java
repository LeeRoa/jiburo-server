package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.domain.admin.domain.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {

    Optional<AdminRole> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT DISTINCT r FROM AdminRole r LEFT JOIN FETCH r.menus ORDER BY r.systemRole DESC, r.id ASC")
    List<AdminRole> findAllWithMenus();
}
