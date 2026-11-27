package com.jiburo.server.domain.admin.domain;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 역할별 메뉴 권한. 행이 있으면 노출(READ), canWrite=true면 편집(WRITE)까지.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_role_menus", uniqueConstraints = @UniqueConstraint(
        name = "uq_admin_role_menu", columnNames = {"role_id", "menu"}))
public class AdminRoleMenu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("관리자 메뉴 (AdminMenu)")
    private AdminMenu menu;

    @Column(name = "can_write", nullable = false)
    @Comment("편집(변경) 권한 여부 — false면 노출(조회)만")
    private boolean canWrite;

    public AdminRoleMenu(AdminRole role, AdminMenu menu, boolean canWrite) {
        this.role = role;
        this.menu = menu;
        this.canWrite = canWrite;
    }

    public AdminAccessLevel level() {
        return canWrite ? AdminAccessLevel.WRITE : AdminAccessLevel.READ;
    }
}
