package com.jiburo.server.domain.user.jwt;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.event.AuditLogEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 1. CustomOAuth2UserService에서 이미 저장/갱신한 사용자 조회
        User dbUser = resolveUser(authToken, oAuth2User);

        // 2. 토큰 발급 (Refresh Token은 Redis에 저장됨)
        TokenResponseDto tokenDto = authTokenService.issueTokens(dbUser);

        // 3. 로그 발행
        publishLoginLog(request, dbUser);

        // 4. 리다이렉트
        // TODO 토큰이 URL 쿼리에 남는 문제 -> 1회용 code 교환 방식으로 전환 필요
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenDto.accessToken())
                .queryParam("refreshToken", tokenDto.refreshToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User resolveUser(OAuth2AuthenticationToken authToken, OAuth2User oAuth2User) {
        if (oAuth2User instanceof CustomOAuth2User customUser && customUser.getUserId() != null) {
            return userRepository.findById(customUser.getUserId())
                    .orElseGet(() -> findOrJoinByOauthId(authToken, oAuth2User));
        }
        return findOrJoinByOauthId(authToken, oAuth2User);
    }

    private User findOrJoinByOauthId(OAuth2AuthenticationToken authToken, OAuth2User oAuth2User) {
        String oauthId = authToken.getAuthorizedClientRegistrationId() + "_" + oAuth2User.getName();
        return userRepository.findByOauthId(oauthId)
                .orElseGet(() -> joinNewUser(oAuth2User, oauthId));
    }

    private User joinNewUser(OAuth2User oAuth2User, String oauthId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String email = (String) attributes.get("email");

        if (name == null) name = "User_" + UUID.randomUUID().toString().substring(0, 8);

        User newUser = User.builder()
                .oauthId(oauthId)
                .nickname(name)
                .email(email != null ? email : "")
                .profileImageUrl(picture)
                .roleCode(RoleType.USER)
                .build();

        return userRepository.save(newUser);
    }

    private void publishLoginLog(HttpServletRequest request, User user) {
        try {
            eventPublisher.publishEvent(AuditLogEvent.builder()
                    .userId(user.getId())
                    .action(LogActionType.AUTH_LOGIN)
                    .clientIp(request.getRemoteAddr())
                    .targetData("Login: " + user.getNickname())
                    .build());
        } catch (Exception e) {
            log.error("로그 발행 실패", e);
        }
    }
}
