package com.jiburo.server.domain.admin.service;

import com.jiburo.server.domain.admin.domain.AdminAccessLevel;
import com.jiburo.server.domain.admin.domain.AdminMenu;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 관리자 API의 @PreAuthorize에서 쓰는 권한 검사 빈 (빈 이름: adminAccess).
 *
 * <pre>
 * @PreAuthorize("@adminAccess.read('SETTING')")   // 메뉴 노출(조회) 권한
 * @PreAuthorize("@adminAccess.write('SETTING')")  // 메뉴 편집(변경) 권한
 * @PreAuthorize("@adminAccess.any()")             // 관리자 역할이 하나라도 있으면
 * </pre>
 * 메뉴 이름은 AdminMenu enum 이름. 오타는 AdminControllerSecurityConventionTest가 잡는다.
 */
@Component("adminAccess")
@RequiredArgsConstructor
public class AdminAccessEvaluator {

    private final AdminAuthorizationService adminAuthorizationService;

    public boolean read(String menu) {
        return adminAuthorizationService.can(currentUserId(), AdminMenu.valueOf(menu), AdminAccessLevel.READ);
    }

    public boolean write(String menu) {
        return adminAuthorizationService.can(currentUserId(), AdminMenu.valueOf(menu), AdminAccessLevel.WRITE);
    }

    public boolean any() {
        return adminAuthorizationService.hasAnyRole(currentUserId());
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return user.getUserId();
        }
        return null;
    }
}
