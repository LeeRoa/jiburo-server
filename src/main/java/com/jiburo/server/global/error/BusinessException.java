package com.jiburo.server.global.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        // 부모(RuntimeException)에게 메시지 키를 넘겨줌 (로깅용)
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
    }
}