package com.jiburo.server.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jiburo.server.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final int status;
    private final String errorCode;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, int status, String errorCode, String message, T data) {
        this.success = success;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    // ── 성공 응답 ──

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), null, null, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, HttpStatus.OK.value(), null, null, null);
    }

    // ── 실패 응답 ──

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getStatus().value(), errorCode.name(), message, null);
    }

    public static <T> ApiResponse<T> fail(int status, String message) {
        return new ApiResponse<>(false, status, null, message, null);
    }
}