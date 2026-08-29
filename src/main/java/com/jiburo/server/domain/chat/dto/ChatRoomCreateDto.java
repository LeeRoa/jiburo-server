package com.jiburo.server.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiburo.server.domain.chat.domain.ChatRoom;
import com.jiburo.server.domain.post.domain.LostPost;
import com.jiburo.server.domain.user.domain.User;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomCreateDto(
        @Schema(description = "어떤 게시글을 보고 채팅을 시작했는지 (게시글 ID, 인코딩된 문자열)") String postId
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
