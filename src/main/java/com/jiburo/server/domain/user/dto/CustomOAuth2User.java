package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final Long userId;
    private final String email;
    private final String roleCode;

    /**
     * 부모(DefaultOAuth2User) 생성자를 호출하고, 우리에게 필요한 userId를 추가로 세팅합니다.
     */
    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            User user) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = user.getId();
        this.email = user.getEmail();
        this.roleCode = user.getRoleCode();
    }
}