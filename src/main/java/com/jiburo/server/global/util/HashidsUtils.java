package com.jiburo.server.global.util;

import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.error.ErrorCode;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DB의 순차적인 Long ID를 외부 노출용 난수 문자열로 변환하는 유틸리티
 */
@Component
public class HashidsUtils {

    private static Hashids hashids;

    public HashidsUtils(
            @Value("${app.hashids.salt:default-salt}") String salt,
            @Value("${app.hashids.min-length:8}") int minLength) {

        HashidsUtils.hashids = new Hashids(salt, minLength);
    }

    /**
     * [인코딩] Long 타입의 DB PK를 암호화된 문자열로 변환.
     * 예: 105L -> "aB7x9YqZ"
     */
    public static String encode(Long id) {
        if (id == null) {
            return null;
        }
        return hashids.encode(id);
    }

    /**
     * [디코딩] 암호화된 문자열을 다시 Long 타입의 DB PK로 복호화.
     * 클라이언트로부터 받은 문자열 주소를 DB 조회용 ID로 바꿀 때 사용.
     */
    public static Long decode(String hash) {
        // 빈 값도 잘못된 식별자로 처리 (null을 돌려주면 findById(null)에서 500이 남)
        if (hash == null || hash.isBlank()) {
            throw new JiburoException(ErrorCode.INVALID_IDENTIFIER);
        }

        long[] decoded;
        try {
            decoded = hashids.decode(hash);
        } catch (IllegalArgumentException e) {
            // alphabet에 없는 문자가 섞인 경우
            throw new JiburoException(ErrorCode.INVALID_IDENTIFIER);
        }

        // 복호화에 실패했거나 (잘못된 문자열), 결과가 없거나, 숫자 여러 개를 담은 hash인 경우.
        // 여러 숫자를 담은 hash([id, x])도 라이브러리상 유효해서 첫 값만 쓰면 같은 ID를 가리키는 다른 문자열(별칭)이 생긴다
        // → STOMP destination처럼 문자열 자체가 채널 키로 쓰이는 곳에서 어긋남. 한 ID = 한 문자열만 허용한다.
        if (decoded.length != 1) {
            throw new JiburoException(ErrorCode.INVALID_IDENTIFIER);
        }

        // 다시 인코딩한 값과 정확히 같은 정규 문자열만 허용 (그 밖의 별칭 형태도 차단)
        if (!hash.equals(hashids.encode(decoded[0]))) {
            throw new JiburoException(ErrorCode.INVALID_IDENTIFIER);
        }

        return decoded[0];
    }
}
