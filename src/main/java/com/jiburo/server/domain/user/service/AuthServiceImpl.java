package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.dto.KakaoLoginRequestDto;
import com.jiburo.server.domain.user.dto.OAuthAttributes;
import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_REGISTRATION_ID = "kakao";
    private static final String KAKAO_USER_NAME_ATTRIBUTE = "id";

    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest request;
    private final RestClient kakaoRestClient;

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto reissue(TokenRequestDto requestDto) {
        // Refresh Token만으로 사용자를 식별하고, Redis 저장값과 비교 후 새 토큰 쌍으로 교체한다.
        // (이전 버전처럼 만료된 Access Token의 클레임을 신뢰하지 않음)
        TokenResponseDto tokenDto = authTokenService.reissue(requestDto.refreshToken());

        // 재발급 요청은 인증 전 시점이라 @AuditLog AOP가 userId를 알 수 없음 -> 새 토큰에서 꺼내 직접 발행
        publishAuthLog(authTokenService.getUserId(tokenDto.accessToken()), LogActionType.AUTH_REISSUE, "Token Reissue");
        return tokenDto;
    }

    @Override
    @Transactional
    public TokenResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        // 1. 안드로이드 Kakao SDK 액세스 토큰으로 카카오 서버에서 사용자 정보 조회
        Map<String, Object> kakaoUserInfo = fetchKakaoUserInfo(requestDto.accessToken());
        OAuthAttributes attributes = OAuthAttributes.of(KAKAO_REGISTRATION_ID, KAKAO_USER_NAME_ATTRIBUTE, kakaoUserInfo);

        // 2. 최초 로그인이면 신규 가입, 아니면 프로필 갱신
        User user = findOrCreateUser(attributes);

        // 3. 토큰 발급 (Refresh Token은 Redis에 저장됨)
        TokenResponseDto tokenDto = authTokenService.issueTokens(user);

        // 4. 로그인 감사 로그 발행 (@AuditLog AOP가 잡을 수 없는 로그인 시점이라 직접 발행)
        publishLoginLog(user);

        return tokenDto;
    }

    @Override
    public void logout(UUID userId, String accessToken) {
        authTokenService.logout(userId, accessToken);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchKakaoUserInfo(String kakaoAccessToken) {
        try {
            return kakaoRestClient
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
        publishAuthLog(user.getId(), LogActionType.AUTH_LOGIN, "Login(Kakao App): " + user.getNickname());
    }

    private void publishAuthLog(UUID userId, LogActionType action, String targetData) {
        try {
            eventPublisher.publishEvent(AuditLogEvent.builder()
                    .userId(userId)
                    .action(action)
                    .clientIp(request.getRemoteAddr())
                    .targetData(targetData)
                    .build());
        } catch (Exception e) {
            log.error("로그 발행 실패", e);
        }
    }
}
