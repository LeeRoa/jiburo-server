package com.jiburo.server.domain.admin.repository;

import com.jiburo.server.domain.admin.domain.AdminPushPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminPushPreferenceRepository extends JpaRepository<AdminPushPreference, UUID> {
}
