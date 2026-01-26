package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
        // 1. 소셜 서비스(구글, 카카오)에서 사용자 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 서비스 구분 (google, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. 키가 되는 필드값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 4. 속성 담기 (OAuthAttributes는 기존에 구현한 것 그대로 사용)
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 5. 사용자 저장 또는 업데이트 (여기서 영속화된 User 객체를 받음)
        User user = saveOrUpdate(attributes);

        // 6. 결과 반환 [핵심 변경]
        // DefaultOAuth2User 대신 -> CustomOAuth2User를 리턴하면서 'user' 엔티티를 통째로 넘김
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                user // <--- User 엔티티 전달 (여기 안에 ID가 들어있음)
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        // 1. 먼저 조회 시도
        User user = userRepository.findByOauthId(attributes.oauthId())
                .map(entity -> entity.updateProfile(attributes.nickname(), attributes.profileImageUrl()))
                .orElse(null);

        // 2. 이미 있으면 저장(업데이트) 후 반환
        if (user != null) {
            return userRepository.save(user);
        }

        // 3. 없으면 새로 생성 (여기서 동시성 이슈 발생 가능)
        try {
            return userRepository.save(attributes.toEntity());
        } catch (DataIntegrityViolationException e) {
            // 동시에 요청이 와서 이미 누가 넣었다면? -> 다시 조회해서 리턴 (로그인 성공 처리)
            return userRepository.findByOauthId(attributes.oauthId())
                    .orElseThrow(() -> new IllegalArgumentException("계정 생성 실패"));
        }
    }
}