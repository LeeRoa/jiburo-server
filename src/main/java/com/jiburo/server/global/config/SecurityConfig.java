package com.jiburo.server.global.config;

import com.jiburo.server.domain.user.jwt.JwtAccessDeniedHandler;
import com.jiburo.server.domain.user.jwt.JwtAuthenticationEntryPoint;
import com.jiburo.server.domain.user.jwt.JwtFilter;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.domain.user.jwt.OAuth2SuccessHandler;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.domain.user.service.AuthTokenService;
import com.jiburo.server.domain.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
// 컨트롤러가 *ControllerDocs 인터페이스를 구현하므로, @PreAuthorize 프록시를 클래스 기반으로 고정해야
// 요청 매핑이 컨트롤러를 인식한다 (인터페이스 기반 JDK 프록시면 핸들러를 못 찾아 404)
@EnableMethodSecurity(proxyTargetClass = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final AuthTokenService authTokenService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // [1] Exception Handling 추가
                // 인증 실패 시(401) 우리가 만든 EntryPoint가 동작하도록 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        // 권한 부족 시(403)도 ApiResponse JSON으로 응답
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 관리자 API: 경로 단위로 일괄 보호. 새 관리자 기능은 /api/v1/admin/** 아래에 두기만 하면 된다.
                        // (가장 먼저 선언해야 아래 permitAll 규칙에 가로채이지 않음)
                        .requestMatchers("/api/v1/admin/**").hasRole(RoleType.ADMIN.getCode())
                        // 게시글: 조회(GET)는 비로그인 허용, 작성/수정/삭제는 인증 필요
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .requestMatchers(
                                "/ws-jiburo/**",
                                "/api/v1/common-codes/**",
                                "/api/v1/settings/public",  // 클라이언트 공개 운영 설정
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/favicon.ico", "/error",
                                "/api/v1/auth/**",
                                "/oauth2/authorization/**", // 로그인 시작 URL
                                "/login/oauth2/code/**"     // 로그인 완료 후 코드 받는 URL
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )

                // [2] JWT Filter를 "UsernamePasswordAuthenticationFilter" 앞에 끼워넣기
                // Username..Filter는 폼 로그인을 담당하는데, 우리는 그 전에 토큰 검사를 먼저 해야 함
                .addFilterBefore(new JwtFilter(jwtTokenProvider, authTokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 허용 origin 목록 (app.cors.allowed-origins, 콤마 구분, 와일드카드 패턴 지원)
     * - 운영: CORS_ALLOWED_ORIGINS 환경변수로 주입
     * - 기본값: 운영 웹 도메인 + 로컬 프론트 개발 서버
     * 모바일 앱(네이티브 HTTP 클라이언트)은 Origin 헤더를 보내지 않아 CORS 영향을 받지 않는다.
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
