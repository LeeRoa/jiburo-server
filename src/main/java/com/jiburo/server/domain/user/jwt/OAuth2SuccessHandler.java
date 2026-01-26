package com.jiburo.server.domain.user.jwt;

import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.global.domain.CodeConst;
import com.jiburo.server.global.log.event.AuditLogEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final String redirectUri;
    private final ApplicationEventPublisher eventPublisher;

    public OAuth2SuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.oauth2.authorized-redirect-uri}") String redirectUri,
            ApplicationEventPublisher eventPublisher
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redirectUri = redirectUri;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException, ServletException {

        // 1. 토큰 생성
        TokenResponseDto tokenResponseDto = jwtTokenProvider.generateTokenDto(authentication);

        // ================= [로그 찍기 시작] =================
        try {
            Long userId = 0L;
            String userName = "Unknown";

            Object principal = authentication.getPrincipal();

            // [핵심 변경] CustomOAuth2User인지 확인하고 ID 꺼내기
            if (principal instanceof CustomOAuth2User customUser) {
                userId = customUser.getUserId(); // 진짜 DB PK 획득
                userName = customUser.getName();
            } else if (principal instanceof OAuth2User oAuth2User) {
                // 혹시 모를 상황 대비 (DefaultOAuth2User가 들어왔을 때)
                userName = oAuth2User.getName();
                log.warn("CustomOAuth2User가 아닌 Principal이 들어왔습니다: {}", principal.getClass());
            }

            // 로그 이벤트 발행
            eventPublisher.publishEvent(AuditLogEvent.builder()
                    .userId(userId)
                    .action(CodeConst.LogAction.AUTH_LOGIN)
                    .clientIp(request.getRemoteAddr())
                    .targetData("Social Login Success (" + userName + ")")
                    .build());

        } catch (Exception e) {
            // 로그 찍다 실패해도 로그인은 성공시켜야 함 (에러 로그만 남김)
            log.error("감사 로그 발행 실패", e);
        }
        // ================= [로그 찍기 끝] =================

        // 2. 리다이렉트 URI 생성
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenResponseDto.accessToken())
                .queryParam("refreshToken", tokenResponseDto.refreshToken())
                .build().toUriString();

        log.info("Social Login Success! Redirecting to: {}", targetUrl);

        // 3. 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}