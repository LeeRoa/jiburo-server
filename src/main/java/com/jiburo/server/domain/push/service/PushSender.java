package com.jiburo.server.domain.push.service;

import java.util.List;
import java.util.Map;

/**
 * 실제 발송 수단 (FCM). 키가 설정되지 않은 환경에서는 로그만 남기는 구현이 쓰인다.
 */
public interface PushSender {

    record PushMessage(String title, String body, Map<String, String> data) {}

    /** 발송 후 더 이상 유효하지 않은 토큰(앱 삭제 등) 목록을 돌려준다 — 호출 측이 삭제 */
    List<String> send(List<String> tokens, PushMessage message);

    boolean enabled();
}
