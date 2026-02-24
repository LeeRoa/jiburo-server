package com.jiburo.server.global.error;

import lombok.Getter;

@Getter
public class JiburoException extends RuntimeException {

    private final ErrorCode errorCode;

    public JiburoException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
    }
}
