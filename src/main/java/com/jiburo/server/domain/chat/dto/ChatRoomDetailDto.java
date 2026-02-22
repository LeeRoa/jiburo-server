package com.jiburo.server.domain.chat.dto;

import java.util.List;

public record ChatRoomDetailDto(
        String roomId,
        String postId,
        String postTitle,
        String postStatus,        // 게시글의 상태 (실종/제보 등)
        List<ChatMessageResponseDto> messageHistory
) {}