package com.jiburo.server.domain.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jiburo.server.domain.push.service.FcmPushSender;
import com.jiburo.server.domain.push.service.PushSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * FCM 설정. 서비스 계정 키(JSON 내용 또는 파일 경로)가 없으면 푸시를 보내지 않고 로그만 남기는 발송기를 쓴다.
 * (로컬·테스트에서 Firebase 없이도 기동되도록)
 */
@Slf4j
@Configuration
public class PushConfig {

    private static final String APP_NAME = "jiburo";

    @Bean
    public PushSender pushSender(@Value("${app.push.fcm.credentials-json:}") String credentialsJson,
                                 @Value("${app.push.fcm.credentials-path:}") String credentialsPath) {
        if (credentialsJson.isBlank() && credentialsPath.isBlank()) {
            log.info("FCM 키가 설정되지 않아 푸시는 로그로만 남깁니다 (FIREBASE_CREDENTIALS_JSON / FIREBASE_CREDENTIALS_PATH)");
            return new LoggingPushSender();
        }
        try (InputStream in = credentialsJson.isBlank()
                ? new FileInputStream(credentialsPath)
                : new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> a.getName().equals(APP_NAME)).findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
            log.info("FCM 푸시 활성화");
            return new FcmPushSender(FirebaseMessaging.getInstance(app));
        } catch (IOException e) {
            throw new IllegalStateException("Firebase 서비스 계정 키를 읽을 수 없습니다", e);
        }
    }

    /** 키가 없을 때: 발송 내용을 로그로만 남긴다 */
    static class LoggingPushSender implements PushSender {
        @Override
        public List<String> send(List<String> tokens, PushMessage message) {
            log.debug("[푸시 비활성] {}개 기기 — {} / {}", tokens.size(), message.title(), message.body());
            return List.of();
        }

        @Override
        public boolean enabled() {
            return false;
        }
    }
}
