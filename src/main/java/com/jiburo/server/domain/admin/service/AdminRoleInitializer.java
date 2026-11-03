package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminRole;
import com.jiburo.server.domain.admin.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 앱 시작 시 시스템 역할(SUPER_ADMIN)이 없으면 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRoleInitializer implements ApplicationRunner {

    private final AdminRoleRepository adminRoleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureSuperAdminRole();
    }

    @Transactional
    public AdminRole ensureSuperAdminRole() {
        return adminRoleRepository.findByCode(AdminRole.SUPER_ADMIN_CODE)
                .orElseGet(() -> {
                    log.info("시스템 역할 SUPER_ADMIN 생성");
                    return adminRoleRepository.save(new AdminRole(
                            AdminRole.SUPER_ADMIN_CODE, "최고 관리자", "모든 메뉴 접근 (시스템 역할, 수정·삭제 불가)", true));
                });
    }
}
