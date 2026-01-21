package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService 객체 생성 및 유저 정보 로딩
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 로그인 진행 중인 서비스를 구분하는 코드 (google, kakao, naver 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. OAuth2 로그인 진행 시 키가 되는 필드값 (PK) (구글은 "sub", 카카오는 "id")
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 4. OAuthAttributes: OAuth2User의 attribute를 서비스에 맞게 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 5. 사용자 저장 또는 업데이트
        User user = saveOrUpdate(attributes);

        // 6. 결과 반환 (세션에 저장될 객체 - 우리는 JWT 쓸거라 중요도는 낮지만 시큐리티 흐름상 필요)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
                attributes.attributes(),
                attributes.nameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        // DB에서 oauthId로 조회, 없으면 새로 생성(toEntity), 있으면 업데이트
        // TODO (단, 여기서는 User 엔티티에 update 메서드가 있어야 함. 우선은 조회만 진행)
        User user = userRepository.findByOauthId(attributes.oauthId())
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}