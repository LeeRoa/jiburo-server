package com.jiburo.server.domain.user.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final String redirectUri;

    public OAuth2SuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.oauth2.authorized-redirect-uri}") String redirectUri
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException, ServletException {

        // 1. 토큰 생성
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 2. 리다이렉트 URI 생성
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenDto.accessToken())
                .queryParam("refreshToken", tokenDto.refreshToken())
                .build().toUriString();

        log.info("Social Login Success! Redirecting to: {}", targetUrl);

        // 3. 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}