package com.jiburo.server.domain.push.service;

import com.jiburo.server.domain.chat.domain.enums.ChatMsgType;
import com.jiburo.server.domain.notification.domain.NotificationCreatedEvent;
import com.jiburo.server.domain.push.service.PushSender.PushMessage;
import com.jiburo.server.global.util.HashidsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

/**
 * 일반 사용자 푸시: 알림 저장 / 채팅 메시지 저장이 커밋된 뒤 비동기로 발송한다.
 * (롤백된 알림이 푸시로 나가지 않도록 AFTER_COMMIT)
 */
@Component
@RequiredArgsConstructor
public class UserPushListener {

    private static final int CHAT_PREVIEW = 100;

    private final PushService pushService;
    private final PushText pushText;

    /** 채팅 메시지 저장 후 발행 — 보낸 사람을 뺀 (나가지 않은) 참여자에게 */
    public record ChatMessageSentEvent(Long roomId, Long messageId, String senderNickname, ChatMsgType type,
                                       String content, Collection<UUID> recipientIds) {}

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationCreatedEvent e) {
        Object[] args = e.args() == null ? new Object[0] : e.args().split(",", -1);
        String base = "push.noti." + e.typeCode();
        Map<String, String> data = new HashMap<>();
        data.put("type", "NOTIFICATION");
        data.put("typeCode", e.typeCode());
        data.put("notificationId", HashidsUtils.encode(e.notificationId()));
        if (e.targetId() != null) {
            data.put("targetId", HashidsUtils.encode(e.targetId()));
        }
        pushService.sendToUsers(List.of(e.receiverId()), locale -> new PushMessage(
                pushText.get(base + ".title", "push.noti.default.title", locale, args),
                pushText.get(base + ".body", "push.noti.default.body", locale, args),
                data));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChatMessage(ChatMessageSentEvent e) {
        Map<String, String> data = Map.of(
                "type", "CHAT",
                "roomId", HashidsUtils.encode(e.roomId()),
                "messageId", HashidsUtils.encode(e.messageId()));
        pushService.sendToUsers(e.recipientIds(), locale -> new PushMessage(
                pushText.get("push.chat.title", null, locale, e.senderNickname()),
                e.type() == ChatMsgType.TALK
                        ? preview(e.content())
                        : pushText.get("push.chat." + e.type().getCode(), "push.chat.default", locale),
                data));
    }

    private static String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > CHAT_PREVIEW ? text.substring(0, CHAT_PREVIEW) + "…" : text;
    }
}
