package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;
import com.jiburo.server.domain.user.jwt.JwtTokenProvider;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;

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
}
