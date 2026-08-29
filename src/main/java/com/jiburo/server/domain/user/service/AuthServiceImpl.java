package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.log.event.AuditLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_REGISTRATION_ID = "kakao";
    private static final String KAKAO_USER_NAME_ATTRIBUTE = "id";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;
    private final RestClient.Builder restClientBuilder;

    @Override
    @Transactional
    public TokenResponseDto reissue(TokenRequestDto requestDto) {
        // 1. Refresh Token 검증
        // (만료되었거나 조작된 토큰이면 여기서 false가 반환됨 -> 예외 발생)
        if (!jwtTokenProvider.validateToken(requestDto.refreshToken())) {
            throw new JiburoException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Access Token에서 Authentication(유저 정보) 가져오기
        // *중요*: Access Token이 만료되었어도, jwtTokenProvider.getAuthentication() 내부에서
        // claims를 강제로 꺼내오도록 구현했기 때문에 정상적으로 동작합니다.
        Authentication authentication = jwtTokenProvider.getAuthentication(requestDto.accessToken());

        // 3. (선택사항) Redis나 DB에 저장된 Refresh Token이 있다면 여기서 비교
        // String savedToken = redisRepository.findById(authentication.getName());
        // if (!savedToken.equals(requestDto.refreshToken())) throw new BusinessException(...);

        // 4. 새로운 토큰 생성
        TokenResponseDto responseDto = jwtTokenProvider.generateTokenDto(authentication);

        return TokenResponseDto.builder()
                .grantType(responseDto.grantType())
                .accessToken(responseDto.accessToken())
                .refreshToken(responseDto.refreshToken())
                .accessTokenExpiresIn(responseDto.accessTokenExpiresIn())
                .build();
    }

    @Override
    @Transactional
    public TokenResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        // 1. 안드로이드 Kakao SDK 액세스 토큰으로 카카오 서버에서 사용자 정보 조회
        Map<String, Object> kakaoUserInfo = fetchKakaoUserInfo(requestDto.accessToken());
        OAuthAttributes attributes = OAuthAttributes.of(KAKAO_REGISTRATION_ID, KAKAO_USER_NAME_ATTRIBUTE, kakaoUserInfo);

        // 2. 최초 로그인이면 신규 가입, 아니면 프로필 갱신
        User user = findOrCreateUser(attributes);

        // 3. 웹 OAuth2 로그인과 동일한 형태의 Authentication 구성 (JWT sub = UUID)
        CustomOAuth2User principal = new CustomOAuth2User(
                user.getId(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey()))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        // 4. 토큰 발급
        TokenResponseDto tokenDto = jwtTokenProvider.generateTokenDto(authentication);

        // 5. 로그인 감사 로그 발행 (@AuditLog AOP가 잡을 수 없는 로그인 시점이라 직접 발행)
        publishLoginLog(user);

        return tokenDto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchKakaoUserInfo(String kakaoAccessToken) {
        try {
            return restClientBuilder.build()
                    .get()
                    .uri(KAKAO_USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            log.warn("카카오 사용자 정보 조회 실패 (액세스 토큰 만료/위조 가능성)", e);
            throw new JiburoException(ErrorCode.OAUTH_PROVIDER_FAILED);
        }
    }

    private User findOrCreateUser(OAuthAttributes attributes) {
        return userRepository.findByOauthId(attributes.oauthId())
                .map(user -> user.updateProfile(attributes.nickname(), attributes.profileImageUrl()))
                .orElseGet(() -> saveNewUser(attributes));
    }

    private User saveNewUser(OAuthAttributes attributes) {
        try {
            return userRepository.save(attributes.toEntity());
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 사용자의 로그인 요청이 들어와 이미 가입된 경우 -> 다시 조회해서 반환
            return userRepository.findByOauthId(attributes.oauthId())
                    .orElseThrow(() -> new JiburoException(ErrorCode.OAUTH_PROVIDER_FAILED));
        }
    }

    private void publishLoginLog(User user) {
        try {
            eventPublisher.publishEvent(AuditLogEvent.builder()
                    .userId(user.getId())
                    .action(LogActionType.AUTH_LOGIN)
                    .clientIp(request.getRemoteAddr())
                    .targetData("Login(Kakao App): " + user.getNickname())
                    .build());
        } catch (Exception e) {
            log.error("로그 발행 실패", e);
        }
    }
}
