package com.jiburo.server.domain.emoticon.controller;

import com.jiburo.server.domain.emoticon.dto.EmoticonPackResponseDto;
import com.jiburo.server.domain.emoticon.service.EmoticonService;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/emoticons")
@Tag(name = "Emoticon", description = "채팅 이모티콘 API")
public class EmoticonController {

    private final EmoticonService emoticonService;

    @Operation(summary = "채팅 이모티콘 목록",
            description = "노출 중인 묶음과 이모티콘 목록. 전송은 STOMP /pub/chat/rooms/{roomId}에 "
                    + "{messageTypeCode: \"EMOTICON\", content: emoticonId}로 보내며, 수신 메시지의 content는 이미지 URL입니다.")
    @GetMapping
    public ApiResponse<List<EmoticonPackResponseDto>> findCatalog() {
        return ApiResponse.success(emoticonService.findCatalog());
    }
}
