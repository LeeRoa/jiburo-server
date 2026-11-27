package com.jiburo.server.global.util;

import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashidsUtilsTest {

    @BeforeEach
    void setUp() {
        new HashidsUtils("test-salt", 8);
    }

    @Test
    void 인코딩한_값은_다시_디코딩된다() {
        assertThat(HashidsUtils.decode(HashidsUtils.encode(105L))).isEqualTo(105L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "!!!@@@", "zzzzzzzzzzzzzzzz"})
    void 비어있거나_잘못된_식별자는_INVALID_IDENTIFIER(String hash) {
        assertThatThrownBy(() -> HashidsUtils.decode(hash))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_IDENTIFIER));
    }

    @Test
    void 숫자_여러_개를_담은_hash는_첫_값이_같아도_별칭으로_보고_거부한다() {
        // 같은 salt로 [105, 999]를 담은 hash — 라이브러리상 유효하고 첫 값이 105라 예전에는 105의 별칭으로 통과했다
        String alias = new org.hashids.Hashids("test-salt", 8).encode(105L, 999L);
        assertThat(alias).isNotEqualTo(HashidsUtils.encode(105L));

        assertThatThrownBy(() -> HashidsUtils.decode(alias))
                .isInstanceOf(JiburoException.class)
                .satisfies(ex -> assertThat(((JiburoException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_IDENTIFIER));
    }
}
