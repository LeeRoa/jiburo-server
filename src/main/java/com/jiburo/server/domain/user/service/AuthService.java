package com.jiburo.server.domain.user.service;

import com.jiburo.server.domain.user.dto.TokenRequestDto;
import com.jiburo.server.domain.user.dto.TokenResponseDto;

public interface AuthService {

    TokenResponseDto reissue(TokenRequestDto requestDto);
}
