package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuthUserService oAuthUserService;

    // 트랜잭션을 걸지 않는다: 사용자 조회·가입은 OAuthUserService가 자체 트랜잭션으로 처리 (동시 첫 로그인 대응)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 소셜 서비스(구글, 카카오)에서 사용자 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 서비스 구분 (google, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 키가 되는 필드값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 4. 속성 담기
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 5. 사용자 조회 또는 가입 (동시에 같은 계정이 처음 로그인해도 한 계정으로 수렴)
        User user;
        try {
            user = oAuthUserService.findOrCreate(attributes);
        } catch (JiburoException e) {
            // OAuth2 로그인 흐름이 실패 처리(로그인 실패 핸들러)로 넘어가도록 OAuth2 예외로 변환
            throw new OAuth2AuthenticationException(new OAuth2Error("user_registration_failed"), e);
        }

        // 6. 결과 반환 — 사용자 ID를 담은 CustomOAuth2User (OAuth2SuccessHandler가 이 ID로 토큰 발급)
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                user
        );
    }
}
