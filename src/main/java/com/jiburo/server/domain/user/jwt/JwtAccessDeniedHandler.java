package com.jiburo.server.domain.user.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 부족할 때(예: USER가 /api/v1/admin/** 호출) 403을 ApiResponse JSON으로 응답한다.
 * (기본 핸들러는 HTML 에러 페이지/빈 응답을 내려 프론트가 일관되게 처리할 수 없음)
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access Denied: uri={}, reason={}", request.getRequestURI(), accessDeniedException.getMessage());

        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(errorCode, MessageUtils.getMessage(errorCode.getMessageKey()))
        ));
    }
}
