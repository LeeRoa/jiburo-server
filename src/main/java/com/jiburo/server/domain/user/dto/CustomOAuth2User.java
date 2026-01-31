package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID; // ★ 중요

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final UUID userId;
    private final String email;
    private final String roleCode;

    // 1. 로그인 성공 시 (User 엔티티 받음)
    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            User user) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = user.getId();
        this.email = user.getEmail();
        this.roleCode = user.getRoleCode();
    }

    // 2. JWT 필터 시 (UUID userId 받음)
    public CustomOAuth2User(UUID userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities, Map.of("id", userId.toString()), "id");
        this.userId = userId;
        this.email = null;
        this.roleCode = null;
    }
}