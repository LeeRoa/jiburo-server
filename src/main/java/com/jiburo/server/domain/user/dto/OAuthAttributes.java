package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.Role;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.BusinessException;
import lombok.Builder;

import java.util.Map;

import static com.jiburo.server.global.error.ErrorCode.OAUTH_PROVIDER_INVALID;

@Builder
public record OAuthAttributes(
        Map<String, Object> attributes,
        String nameAttributeKey,
        String oauthId,
        String nickname,
        String email,
        String profileImageUrl
) {
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "naver" -> ofNaver(attributes);
            case "kakao" -> ofKakao(userNameAttributeName, attributes);
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            default -> throw new BusinessException(OAUTH_PROVIDER_INVALID);
        };
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

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        // 네이버는 'response' 키 안에 유저 정보가 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .oauthId("naver_" + response.get("id"))
                .nickname((String) response.get("nickname"))
                .email((String) response.get("email"))
                .profileImageUrl((String) response.get("profile_image"))
                .attributes(response) // attributes 자체를 response로 치환하여 관리하기 편하게 함
                .nameAttributeKey("id")
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