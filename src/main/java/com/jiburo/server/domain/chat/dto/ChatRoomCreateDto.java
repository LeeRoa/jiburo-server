package com.jiburo.server.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.util.HashidsUtils;

public record ChatRoomCreateDto(
        String postId  // 어떤 게시물을 보고 채팅을 시작했는지
) {
    public ChatRoom toEntity(LostPost post, User host) {
        return ChatRoom.builder()
                .post(post)
                .host(host)
                .build();
    }

    @JsonIgnore
    public Long getDecodedPostId() {
        return HashidsUtils.decode(this.postId);
    }
}
