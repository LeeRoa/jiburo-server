package com.jiburo.server.global.error;

import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러에서 발생하는 예외 처리
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * [1] BusinessException 처리
     * 서비스 로직에서 개발자가 의도적으로 던진 예외를 처리합니다.
     * 예: throw new BusinessException(ErrorCode.USER_NOT_FOUND);
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus httpStatus = errorCode.getStatus();

        // 다국어 메시지 조회
        String message = MessageUtils.getMessage(errorCode.getMessageKey());

        log.warn("Business Exception: Code={}, Message={}", errorCode, message);

        // ResponseEntity를 사용하여 실제 HTTP 상태 코드와 Body 데이터를 함께 설정
        return ResponseEntity
                .status(httpStatus)
                .body(ApiResponse.fail(httpStatus.value(), message));
    }

    /**
     * [2] @Valid 유효성 검사 실패 처리
     * Request Body의 데이터가 DTO 제약조건을 위반했을 때 발생합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder builder = new StringBuilder();

        // 첫 번째 에러 메시지만 가져오거나, 반복문으로 모두 나열 가능
        // 여기서는 모든 필드 에러를 나열하는 방식 사용
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append("[");
            builder.append(fieldError.getField());
            builder.append("](은)는 ");
            builder.append(fieldError.getDefaultMessage());
            builder.append(". ");
        }

        String message = builder.toString().trim();
        log.warn("Validation Error: {}", message);

        // 유효성 검사 실패는 무조건 400 Bad Request
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * [3] 그 외 모든 예외 처리 (최후의 보루)
     * 예상치 못한 서버 에러(NPE 등)를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        String message = MessageUtils.getMessage(errorCode.getMessageKey());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), message));
    }
}