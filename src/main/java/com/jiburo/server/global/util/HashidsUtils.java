package com.jiburo.server.global.util;

import com.jiburo.server.global.error.BusinessException;
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
        if (hash == null || hash.isBlank()) {
            return null;
        }

        long[] decoded = hashids.decode(hash);

        // 복호화에 실패했거나 (잘못된 문자열), 결과가 없는 경우
        if (decoded.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_IDENTIFIER);
        }

        return decoded[0];
    }
}