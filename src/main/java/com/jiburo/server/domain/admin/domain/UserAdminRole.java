package com.jiburo.server.domain.admin.domain;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계정 ↔ 관리자 역할 (한 계정에 여러 역할 가능)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_admin_roles", uniqueConstraints = @UniqueConstraint(
        name = "uq_user_admin_role", columnNames = {"user_id", "role_id"}))
public class UserAdminRole extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    public UserAdminRole(User user, AdminRole role) {
        this.user = user;
        this.role = role;
    }
}
