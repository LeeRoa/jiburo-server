package com.jiburo.server.domain.admin.domain;

import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 역할 (메뉴 권한의 묶음). 관리자 웹에서 생성/수정/삭제한다.
 * SUPER_ADMIN 같은 시스템 역할(system=true)은 수정·삭제할 수 없고 모든 메뉴에 접근한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_roles", uniqueConstraints = @UniqueConstraint(name = "uq_admin_role_code", columnNames = "code"))
public class AdminRole extends BaseTimeEntity {

    public static final String SUPER_ADMIN_CODE = "SUPER_ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @Comment("역할 코드 (영문 대문자/숫자/_, 생성 후 변경 불가)")
    private String code;

    @Column(nullable = false, length = 100)
    @Comment("역할 이름 (화면 표시용)")
    private String name;

    @Column(length = 500)
    @Comment("설명")
    private String description;

    @Column(name = "system_role", nullable = false)
    @Comment("시스템 역할 여부 (true면 수정/삭제 불가, 전체 권한)")
    private boolean systemRole;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdminRoleMenu> menus = new ArrayList<>();

    public AdminRole(String code, String name, String description, boolean systemRole) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
    }

    public boolean isSuperAdmin() {
        return SUPER_ADMIN_CODE.equals(code);
    }

    public void rename(String name, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
    }

    /**
     * 메뉴 권한 전체 교체. 호출 전에 메뉴별 부여 가능 여부를 검증해야 한다.
     */
    public void replaceMenus(List<AdminRoleMenu> newMenus) {
        this.menus.clear();
        this.menus.addAll(newMenus);
    }
}
