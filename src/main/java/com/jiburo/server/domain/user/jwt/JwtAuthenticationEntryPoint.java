package com.jiburo.server.domain.user.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.util.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(@NonNull HttpServletRequest request, HttpServletResponse response, @NonNull AuthenticationException authException) throws IOException {
        // 유효한 자격증명(토큰)을 제공하지 않고 접근하려 할 때 401 에러 리턴

        // 1. HTTP 상태 코드 설정 (401)
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 2. ApiResponse JSON 생성
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse<Void> apiResponse = ApiResponse.fail(
                errorCode,
                MessageUtils.getMessage(errorCode.getMessageKey())
        );

        // 3. JSON 변환 후 출력
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}