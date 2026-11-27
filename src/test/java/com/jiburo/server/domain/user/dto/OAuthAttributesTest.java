package com.jiburo.server.domain.user.dto;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthAttributesTest {

    @Test
    void 네이버_응답에_nickname이_없으면_name을_닉네임으로_사용() {
        Map<String, Object> naver = Map.of("response", Map.of("id", "abc", "name", "홍길동", "email", "a@naver.com"));

        OAuthAttributes attrs = OAuthAttributes.of("naver", "response", naver);

        assertThat(attrs.oauthId()).isEqualTo("naver_abc");
        assertThat(attrs.nickname()).isEqualTo("홍길동");
        assertThat(attrs.toEntity().getNickname()).isEqualTo("홍길동");
    }

    @Test
    void 카카오_프로필_동의가_없어도_NPE_없이_기본_닉네임으로_가입() {
        Map<String, Object> kakao = Map.of("id", 123L, "kakao_account", Map.of());

        OAuthAttributes attrs = OAuthAttributes.of("kakao", "id", kakao);

        assertThat(attrs.oauthId()).isEqualTo("kakao_123");
        assertThat(attrs.nickname()).isNull();
        assertThat(attrs.toEntity().getNickname()).startsWith("User_");
        assertThat(attrs.email()).isEmpty();
    }

    @Test
    void 카카오_kakao_account_자체가_없어도_처리된다() {
        OAuthAttributes attrs = OAuthAttributes.of("kakao", "id", Map.of("id", 1L));

        assertThat(attrs.profileImageUrl()).isNull();
        assertThat(attrs.toEntity().getNickname()).startsWith("User_");
    }

    @Test
    void 프로필_이미지가_없는_사용자가_재로그인해도_NPE가_나지_않는다() {
        User user = User.builder().oauthId("kakao_1").nickname("기존").profileImageUrl(null).roleCode(RoleType.USER).build();

        user.updateProfile("새닉네임", null);

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(user.getProfileImageUrl()).isNull();
    }

    @Test
    void 제공자가_빈_값을_주면_기존_프로필을_유지한다() {
        User user = User.builder().oauthId("kakao_1").nickname("기존").profileImageUrl("old.png").roleCode(RoleType.USER).build();

        user.updateProfile(null, " ");

        assertThat(user.getNickname()).isEqualTo("기존");
        assertThat(user.getProfileImageUrl()).isEqualTo("old.png");
    }
}
