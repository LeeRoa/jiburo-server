package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.Role;
import com.jiburo.server.domain.user.domain.User;
import lombok.Builder;

import java.util.Map;

/**
 * - 모든 필드는 private final로 자동 선언
 * - Getter 자동 생성 (이름이 getNickname()이 아니라 nickname()이 됨)
 * - 생성자, equals, hashCode, toString 자동 생성
 */
@Builder
public record OAuthAttributes(
        Map<String, Object> attributes,
        String nameAttributeKey,
        String oauthId,
        String nickname,
        String email,
        String profileImageUrl
) {
    // static 팩토리 메서드
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .oauthId("google_" + attributes.get(userNameAttributeName))
                .nickname((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profileImageUrl((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .oauthId("kakao_" + attributes.get(userNameAttributeName))
                .nickname((String) kakaoProfile.get("nickname"))
                .email(kakaoAccount.get("email") != null ? (String) kakaoAccount.get("email") : "")
                .profileImageUrl((String) kakaoProfile.get("profile_image_url"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity() {
        return User.builder()
                .oauthId(oauthId)
                .nickname(nickname)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .role(Role.USER)
                .build();
    }
}