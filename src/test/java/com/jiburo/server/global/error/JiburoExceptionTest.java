package com.jiburo.server.global.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JiburoExceptionTest {

    @Test
    void errorCode를_정상적으로_반환한다() {
        JiburoException ex = new JiburoException(ErrorCode.USER_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 예외_메시지는_messageKey이다() {
        JiburoException ex = new JiburoException(ErrorCode.POST_NOT_FOUND);

        assertThat(ex.getMessage()).isEqualTo(ErrorCode.POST_NOT_FOUND.getMessageKey());
    }

    @Test
    void HTTP_상태는_errorCode에서_가져온다() {
        JiburoException ex = new JiburoException(ErrorCode.UNAUTHORIZED);

        assertThat(ex.getErrorCode().getStatus().value()).isEqualTo(401);
    }
}
