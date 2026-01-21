package com.jiburo.server.global.error;

import com.jiburo.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러의 예외를 여기서 처리함
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * [1] @Valid 유효성 검사 실패 시 발생
     * 주로 DTO의 제약조건(@NotNull, @Size 등) 위반 시
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder builder = new StringBuilder();

        // 에러가 여러 개일 경우 첫 번째 메시지만 보내거나, 모두 보낼 수 있음
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append("[");
            builder.append(fieldError.getField());
            builder.append("](은)는 ");
            // DTO에 설정한 메시지(예: {valid.email.empty})가 이미 다국어 처리되어 들어옴
            builder.append(fieldError.getDefaultMessage());
            builder.append(" ");
        }

        log.warn("Validation Error: {}", builder);
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), builder.toString().trim());
    }

    /**
     * [2] 비즈니스 로직 예외 (IllegalArgumentException 등)
     * 예: throw new IllegalArgumentException("error.user.not_found");
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        // 예외 메시지로 들어온 키값(예: error.user.not_found)을 다국어 메시지로 변환
        String messageKey = e.getMessage();
        String translatedMessage = getMessage(messageKey);

        log.warn("Business Error: {}", translatedMessage);
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), translatedMessage);
    }

    /**
     * [3] 나머지 모든 예외
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("Unknown Error: ", e); // 서버 로그에는 전체 스택트레이스 남김

        // 사용자에게는 "서버 내부 오류입니다" 같은 공통 메시지 전달
        String message = getMessage("error.server.internal");
        return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    // 다국어 메시지 조회 헬퍼 메서드
    private String getMessage(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, null, locale);
        } catch (Exception e) {
            // 메시지 코드를 못 찾으면 코드 그 자체를 반환
            return code;
        }
    }
}