package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.domain.user.domain.enums.RoleType;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 계정 정지 / 해제.
 * - 정지되면 로그인·토큰 재발급이 막힌다 (AuthTokenService.issueTokens).
 * - Refresh Token을 지우고 이미 받은 Access Token도 즉시 무효화한다 (AuthTokenService.revokeAllTokens).
 * - 관리자 계정은 정지할 수 없다 (먼저 관리자 역할을 회수해야 함).
 */
@Service
@RequiredArgsConstructor
public class UserSuspensionService {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;

    /** days가 null이면 영구 정지. 반환값은 정지 만료 시각 */
    @Transactional
    public LocalDateTime suspend(UUID actorId, UUID userId, Integer days, String reason) {
        if (userId.equals(actorId)) {
            throw new JiburoException(ErrorCode.INVALID_INPUT_VALUE);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
        if (user.getRoleCode() == RoleType.ADMIN) {
            throw new JiburoException(ErrorCode.ACCESS_DENIED);
        }
        LocalDateTime until = days == null ? User.PERMANENT_SUSPENSION : LocalDateTime.now().plusDays(days);
        // 이미 더 긴 정지가 걸려 있으면 줄이지 않는다
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(until)) {
            until = user.getSuspendedUntil();
        }
        user.suspend(until, reason);
        // 재발급을 끊고, 이미 받은 Access Token도 즉시 무효화 (REST·STOMP 모두 다음 요청부터 거부)
        authTokenService.revokeAllTokens(userId);
        return until;
    }

    @Transactional
    public void unsuspend(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
        user.unsuspend();
    }
}
