package com.jiburo.server.domain.user.jwt;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.log.event.AuditLogEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();


        String provider = authToken.getAuthorizedClientRegistrationId();
        String providerId = oAuth2User.getName();
        String oauthId = provider + "_" + providerId;

        User dbUser = userRepository.findByOauthId(oauthId)
                .orElseGet(() -> joinNewUser(oAuth2User, oauthId));

        // 2. UUID를 담은 CustomUser 생성
        CustomOAuth2User internalUser = new CustomOAuth2User(
                dbUser.getId(),
                oAuth2User.getAuthorities()
        );

        // 3. 인증 객체 생성
        Authentication dbAuthentication = new UsernamePasswordAuthenticationToken(
                internalUser,
                null,
                oAuth2User.getAuthorities()
        );

        // 4. 토큰 생성 (UUID가 문자열로 변환되어 들어감)
        TokenResponseDto tokenDto = jwtTokenProvider.generateTokenDto(dbAuthentication);

        // 5. 로그 발행 (UUID userId 기록)
        publishLoginLog(request, dbUser);

        // 6. 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenDto.accessToken())
                .queryParam("refreshToken", tokenDto.refreshToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
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
                .roleCode("USER")
                .build();

        return userRepository.save(newUser);
    }

    private void publishLoginLog(HttpServletRequest request, User user) {
        try {
            eventPublisher.publishEvent(AuditLogEvent.builder()
                    .userId(user.getId())
                    .action(CodeConst.LogAction.AUTH_LOGIN)
                    .clientIp(request.getRemoteAddr())
                    .targetData("Login: " + user.getNickname())
                    .build());
        } catch (Exception e) {
            log.error("로그 발행 실패", e);
        }
    }
}