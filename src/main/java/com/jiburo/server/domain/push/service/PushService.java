package com.jiburo.server.domain.push.service;

import com.jiburo.server.domain.push.domain.DevicePlatform;
import com.jiburo.server.domain.push.domain.PushDevice;
import com.jiburo.server.domain.push.repository.PushDeviceRepository;
import com.jiburo.server.domain.push.service.PushSender.PushMessage;
import com.jiburo.server.domain.user.dao.UserRepository;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.error.ErrorCode;
import com.jiburo.server.global.error.JiburoException;
import com.jiburo.server.global.i18n.MessageCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 기기 등록과 사용자 단위 발송.
 * 기기마다 등록 시 언어가 있어서, 같은 알림도 기기 언어별로 문구를 만들어 보낸다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final PushDeviceRepository pushDeviceRepository;
    private final UserRepository userRepository;
    private final PushSender pushSender;

    /** 로그인 후 / 토큰 갱신 시 호출. 같은 토큰이 다른 계정에 있으면 이 계정으로 옮긴다 */
    @Transactional
    public void register(UUID userId, String token, DevicePlatform platform, Locale locale) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JiburoException(ErrorCode.USER_NOT_FOUND));
        String tag = locale == null ? MessageCatalog.BASE_LOCALE : MessageCatalog.normalizeTag(locale.toLanguageTag());
        if (tag.isBlank() || "und".equals(tag) || tag.length() > 20) {
            tag = MessageCatalog.BASE_LOCALE;
        }
        String localeTag = tag;
        pushDeviceRepository.findByToken(token).ifPresentOrElse(
                d -> d.reassign(user, platform, localeTag),
                () -> pushDeviceRepository.save(new PushDevice(user, token, platform, localeTag)));
    }

    /** 로그아웃 시 호출 — 이 기기로 더 이상 이 계정 푸시를 받지 않음 */
    @Transactional
    public void unregister(UUID userId, String token) {
        pushDeviceRepository.deleteByTokenAndUserId(token, userId);
    }

    /**
     * 사용자들에게 발송. 문구는 기기 언어별로 messageOf(locale)로 만든다.
     * 호출 측에서 비동기로 부른다 (요청 처리 시간에 FCM 왕복이 섞이지 않도록).
     */
    public void sendToUsers(Collection<UUID> userIds, Function<Locale, PushMessage> messageOf) {
        if (userIds.isEmpty()) {
            return;
        }
        sendToTargets(targetsOf(userIds), messageOf);
    }

    /** 발송 대상 기기 (토큰 + 문구 언어). DB 장애 대비로 미리 받아 둘 때 사용 */
    public record DeviceTarget(String token, String locale) {}

    public List<DeviceTarget> targetsOf(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return pushDeviceRepository.findAllByUserIds(userIds).stream()
                .map(d -> new DeviceTarget(d.getToken(), d.getLocale()))
                .toList();
    }

    /** 기기 목록으로 직접 발송 (DB 조회 없음 — 만료 토큰 삭제만 시도) */
    public void sendToTargets(List<DeviceTarget> targets, Function<Locale, PushMessage> messageOf) {
        if (targets.isEmpty()) {
            return;
        }
        Map<String, List<String>> tokensByLocale = targets.stream().collect(Collectors.groupingBy(
                DeviceTarget::locale, Collectors.mapping(DeviceTarget::token, Collectors.toList())));

        List<String> dead = new ArrayList<>();
        tokensByLocale.forEach((tag, tokens) -> {
            try {
                dead.addAll(pushSender.send(tokens, messageOf.apply(Locale.forLanguageTag(tag))));
            } catch (RuntimeException e) {
                log.warn("푸시 발송 중 오류 (locale={}, {}개): {}", tag, tokens.size(), e.getMessage());
            }
        });
        if (!dead.isEmpty()) {
            try {
                pushDeviceRepository.deleteByTokens(dead);
                log.info("만료된 푸시 토큰 {}개 삭제", dead.size());
            } catch (RuntimeException e) {
                log.warn("만료된 푸시 토큰 삭제 실패 (다음 발송 때 다시 시도): {}", e.getMessage());
            }
        }
    }

    public long deviceCount(UUID userId) {
        return pushDeviceRepository.countByUser_Id(userId);
    }
}
