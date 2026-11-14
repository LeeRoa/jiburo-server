package com.jiburo.server.domain.chat.config;

import com.jiburo.server.global.util.HashidsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * 채팅방 구독 회수.
 * 구독 권한은 SUBSCRIBE 시점에만 검사하므로, 방을 나간 사용자의 기존 구독은 그대로 남아 메시지를 계속 받는다.
 * 방을 나갈 때 이 컴포넌트로 그 사용자의 해당 방 구독(/sub/chat/rooms/{roomId}, /read)을 브로커에서 지운다.
 */
@Slf4j
@Component
public class ChatSubscriptionRevoker {

    private final SimpUserRegistry userRegistry;
    private final MessageChannel brokerChannel;

    public ChatSubscriptionRevoker(SimpUserRegistry userRegistry,
                                   @Qualifier("brokerChannel") MessageChannel brokerChannel) {
        this.userRegistry = userRegistry;
        this.brokerChannel = brokerChannel;
    }

    /** 트랜잭션 안이면 커밋 후에, 아니면 바로 회수한다 (롤백되면 구독을 지우지 않도록) */
    public void revokeRoomAfterCommit(UUID userId, Long roomId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    revokeRoom(userId, roomId);
                }
            });
        } else {
            revokeRoom(userId, roomId);
        }
    }

    public void revokeRoom(UUID userId, Long roomId) {
        SimpUser user = userRegistry.getUser(userId.toString());
        if (user == null) {
            return;
        }
        for (SimpSession session : new ArrayList<>(user.getSessions())) {
            for (SimpSubscription subscription : new ArrayList<>(session.getSubscriptions())) {
                if (Objects.equals(roomIdOf(subscription.getDestination()), roomId)) {
                    unsubscribe(session.getId(), subscription.getId(), subscription.getDestination());
                }
            }
        }
    }

    private Long roomIdOf(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = StompHandler.CHAT_ROOM_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return HashidsUtils.decode(matcher.group(1));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void unsubscribe(String sessionId, String subscriptionId, String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        brokerChannel.send(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
        log.debug("채팅방 구독 회수: sessionId={}, destination={}", sessionId, destination);
    }
}
