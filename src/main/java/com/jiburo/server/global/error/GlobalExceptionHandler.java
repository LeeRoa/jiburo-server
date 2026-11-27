package com.jiburo.server.global.error;

import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 ApiResponse.fail() 형태로 변환한다.
 * 클라이언트 입력 문제(잘못된 JSON, 타입 불일치, 없는 경로 등)는 4xx로 응답하고,
 * 예상하지 못한 예외만 500 + ERROR 로그로 남긴다.
 */
@Slf4j
@RestControllerAdvice // 모든 컨트롤러에서 발생하는 예외 처리
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // 관리자 모니터링용 최근 500 오류 보관 (단위 테스트에서 new로 만들 때는 없을 수 있음)
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.jiburo.server.global.monitoring.ServerErrorRecorder serverErrorRecorder;

    /**
     * [1] 비즈니스 예외
     * 서비스 로직에서 의도적으로 던진 예외. 예: throw new JiburoException(ErrorCode.USER_NOT_FOUND);
     */
    @ExceptionHandler(JiburoException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(JiburoException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business Exception: Code={}, Message={}", errorCode.name(), MessageUtils.getMessage(errorCode.getMessageKey()));
        return fail(errorCode);
    }

    /**
     * [2] 유효성 검사 실패
     * - @Valid @RequestBody (MethodArgumentNotValidException은 BindException의 하위 타입)
     * - @Valid @ModelAttribute (BindException)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(BindException e) {
        String message = formatFieldErrors(e.getBindingResult());
        log.warn("Validation Error: {}", message);
        return fail(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    // [2-1] @PathVariable/@RequestParam 등 메서드 파라미터 제약 위반 (Spring 6.1+)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        log.warn("Parameter Validation Error: {}", e.getMessage());
        return fail(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * [3] 요청 본문을 읽을 수 없음 — 깨진 JSON, 존재하지 않는 enum 값(예: statusCode="FOO"), 타입 불일치
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Unreadable Request Body: {}", e.getMostSpecificCause().getMessage());
        return fail(ErrorCode.INVALID_INPUT_VALUE);
    }

    // [4] 쿼리 파라미터/경로 변수 타입 불일치 — 예: ?categoryCode=FOO, ?page=abc
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type Mismatch: parameter={}, value={}", e.getName(), e.getValue());
        return fail(ErrorCode.INVALID_TYPE_VALUE);
    }

    // [5] 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Missing Parameter: {}", e.getParameterName());
        return fail(ErrorCode.INVALID_INPUT_VALUE);
    }

    // [6] 지원하지 않는 HTTP 메서드 (예: GET만 있는 경로에 DELETE)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method Not Supported: {}", e.getMethod());
        return fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // [7] 지원하지 않는 Content-Type (예: JSON API에 form-data 전송)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("Media Type Not Supported: {}", e.getContentType());
        return fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    // [8] 존재하지 않는 경로 (Spring Boot 3.2+는 404를 NoResourceFoundException으로 던짐 -> 기존엔 500으로 응답됨)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        log.debug("No Resource: {}", e.getResourcePath());
        return fail(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // [9] 메서드 보안(@PreAuthorize 등) 권한 부족
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return fail(ErrorCode.ACCESS_DENIED);
    }

    /**
     * [10] 그 외 모든 예외 (최후의 보루) — 예상치 못한 서버 에러(NPE 등)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unhandled Exception: ", e);
        if (serverErrorRecorder != null) {
            serverErrorRecorder.record(request.getMethod(), request.getRequestURI(), e);
        }
        return fail(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // ── helpers ──

    private ResponseEntity<ApiResponse<Void>> fail(ErrorCode errorCode) {
        return fail(errorCode, MessageUtils.getMessage(errorCode.getMessageKey()));
    }

    private ResponseEntity<ApiResponse<Void>> fail(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode, message));
    }

    private String formatFieldErrors(BindingResult bindingResult) {
        StringBuilder builder = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append("[")
                    .append(fieldError.getField())
                    .append("](은)는 ")
                    .append(fieldError.getDefaultMessage())
                    .append(". ");
        }
        // 필드가 아닌 객체 단위 검증(@AssertTrue 메서드 등)
        bindingResult.getGlobalErrors().forEach(error -> builder.append(error.getDefaultMessage()).append(". "));
        String message = builder.toString().trim();
        return message.isEmpty() ? MessageUtils.getMessage(ErrorCode.INVALID_INPUT_VALUE.getMessageKey()) : message;
    }
}
