package com.jiburo.server.domain.chat.dto;

import com.jiburo.server.global.util.HashidsUtils;

import java.util.UUID;

public class ChatReadDto {
    /**
     * 프론트엔드 -> 서버 (요청)
     * "나(현재 세션 유저) 이 방에서 몇 번 메시지까지 읽었어"
     * (누가 보냈는지는 STOMP 세션에서 UUID를 꺼낼 것이므로 DTO에 필요 없음)
     */
    public record Request(
            String lastReadMessageId
    ) {}

    /**
     * 서버 -> 프론트엔드 (응답/브로드캐스트)
     * "이 방에 있는 A 유저가 몇 번 메시지까지 읽었대!"
     * (누가 읽었는지 다른 사람들에게 알려줘야 하므로 userId가 필수 포함됨)
     */
    public record Response(
            UUID userId,           // 읽음 처리를 한 유저의 ID
            String lastReadMessageId // 그 유저가 마지막으로 읽은 메시지 ID
    ) {
        public Response(UUID userId, Long lastReadMessageId) {
            this(userId, HashidsUtils.encode(lastReadMessageId));
        }
    }
}
