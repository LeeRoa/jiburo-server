package com.jiburo.server.global.util;

public interface Translatable {
    // 다국어 메시지 키를 반환하는 메서드 (모든 Enum이 강제 구현)
    String getMessageKey();
}