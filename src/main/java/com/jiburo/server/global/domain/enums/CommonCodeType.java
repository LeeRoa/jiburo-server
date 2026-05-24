package com.jiburo.server.global.domain.enums;

public interface CommonCodeType {
    /**
     * DB의 group_code 컬럼에 들어갈 값 (예: "ROLE", "BADGE")
     */
    String getGroupCode();

    /**
     * DB의 code 컬럼에 들어갈 값 (예: "USER", "BEGINNER")
     */
    String getCode();

    /**
     * 프론트엔드가 다국어 처리에 사용할 키 (예: "role.user")
     */
    String getDescriptionKey();

    /**
     * DB의 value 컬럼에 들어갈 추가 데이터 (폴더 경로, 설정값 등)
     * 기본적으로는 값이 없으므로 null을 반환하도록 default 처리
     */
    default String getValue() {
        return null;
    }
}