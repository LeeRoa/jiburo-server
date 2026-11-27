package com.jiburo.server.domain.push.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FCM 발송. 한 번에 최대 500개 토큰(FCM 멀티캐스트 한도)씩 나눠 보낸다.
 */
@Slf4j
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    private static final int BATCH = 500;
    // 이 오류가 나면 토큰이 더 이상 쓸 수 없는 상태 (앱 삭제, 토큰 갱신 등)
    private static final Set<MessagingErrorCode> DEAD_TOKEN = Set.of(
            MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.SENDER_ID_MISMATCH);

    private final FirebaseMessaging messaging;

    @Override
    public List<String> send(List<String> tokens, PushMessage message) {
        List<String> dead = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += BATCH) {
            List<String> batch = tokens.subList(from, Math.min(from + BATCH, tokens.size()));
            MulticastMessage multicast = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
                    .putAllData(message.data())
                    .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setSound("default").build()).build())
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            try {
                BatchResponse response = messaging.sendEachForMulticast(multicast);
                List<SendResponse> results = response.getResponses();
                for (int i = 0; i < results.size(); i++) {
                    FirebaseMessagingException e = results.get(i).getException();
                    if (e != null && DEAD_TOKEN.contains(e.getMessagingErrorCode())) {
                        dead.add(batch.get(i));
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.warn("FCM 발송 실패 ({}개 토큰): {}", batch.size(), e.getMessage());
            }
        }
        return dead;
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
