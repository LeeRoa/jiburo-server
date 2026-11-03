package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.JiburoException;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

import static com.jiburo.server.global.error.ErrorCode.OAUTH_PROVIDER_FAILED;
import static com.jiburo.server.global.error.ErrorCode.OAUTH_PROVIDER_INVALID;

/**
 * 소셜 제공자별 사용자 정보 정규화.
 * 제공자 응답은 사용자 동의 항목에 따라 필드가 빠질 수 있으므로 모든 값을 null-safe로 꺼낸다.
 * 닉네임은 users.nickname(NOT NULL) 제약 때문에 끝까지 비어 있으면 "User_xxxxxxxx" 형태로 생성한다.
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
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if (attributes == null) {
            throw new JiburoException(OAUTH_PROVIDER_FAILED);
        }
        return switch (registrationId) {
            case "naver" -> ofNaver(attributes);
            case "kakao" -> ofKakao(userNameAttributeName, attributes);
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            default -> throw new JiburoException(OAUTH_PROVIDER_INVALID);
        };
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .oauthId("google_" + requireId(attributes.get(userNameAttributeName)))
                .nickname(firstNonBlank(str(attributes, "name"), str(attributes, "given_name")))
                .email(str(attributes, "email"))
                .profileImageUrl(str(attributes, "picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 사용자가 프로필 제공에 동의하지 않으면 kakao_account / profile 자체가 없을 수 있음
        Map<String, Object> kakaoAccount = map(attributes, "kakao_account");
        Map<String, Object> kakaoProfile = map(kakaoAccount, "profile");
        Map<String, Object> properties = map(attributes, "properties"); // 구버전 응답 호환

        String email = str(kakaoAccount, "email");
        return OAuthAttributes.builder()
                .oauthId("kakao_" + requireId(attributes.get(userNameAttributeName)))
                .nickname(firstNonBlank(str(kakaoProfile, "nickname"), str(properties, "nickname")))
                .email(email != null ? email : "")
                .profileImageUrl(firstNonBlank(str(kakaoProfile, "profile_image_url"), str(properties, "profile_image")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(Map<String, Object> attributes) {
        // 네이버는 'response' 키 안에 유저 정보가 있음
        Map<String, Object> response = map(attributes, "response");

        return OAuthAttributes.builder()
                .oauthId("naver_" + requireId(response.get("id")))
                // 앱 설정(scope)에 따라 nickname 대신 name만 오는 경우가 있어 둘 다 확인
                .nickname(firstNonBlank(str(response, "nickname"), str(response, "name")))
                .email(str(response, "email"))
                .profileImageUrl(str(response, "profile_image"))
                .attributes(response) // attributes 자체를 response로 치환하여 관리하기 편하게 함
                .nameAttributeKey("id")
                .build();
    }

    public User toEntity() {
        return User.builder()
                .oauthId(oauthId)
                .nickname(nickname != null ? nickname : defaultNickname())
                .email(email)
                .profileImageUrl(profileImageUrl)
                .roleCode(RoleType.USER)
                .build();
    }

    public static String defaultNickname() {
        return "User_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // --- null-safe 헬퍼 ---

    private static Object requireId(Object id) {
        if (id == null) {
            throw new JiburoException(OAUTH_PROVIDER_FAILED);
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> source, String key) {
        Object value = source != null ? source.get(key) : null;
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private static String str(Map<String, Object> source, String key) {
        Object value = source != null ? source.get(key) : null;
        return value != null ? value.toString() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
