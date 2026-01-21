package com.jiburo.server.global.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final int status;
    private final String message;
    private final T data;

    // 생성자는 private으로 막고, static 팩토리 메서드만 노출 (명확성)
    private ApiResponse(boolean success, int status, String message, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // 성공 응답 (데이터 있음)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), "Success", data);
    }

    // 성공 응답 (데이터 없음 - 예: 삭제 성공 등)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, HttpStatus.OK.value(), "Success", null);
    }

    // 성공 응답 (메시지 커스텀)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data);
    }

    // 실패 응답 (에러 핸들러에서 사용)
    public static <T> ApiResponse<T> fail(int status, String message) {
        return new ApiResponse<>(false, status, message, null);
    }
}