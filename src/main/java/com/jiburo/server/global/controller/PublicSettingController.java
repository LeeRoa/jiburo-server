package com.jiburo.server.global.controller;

import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.setting.AppSettingService;
import com.jiburo.server.global.setting.SettingKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 클라이언트(앱/웹)가 미리 알아야 하는 운영 제한값 공개 API.
 * 예: 이미지 최대 장수, 파일 최대 크기, 채팅 최대 길이 — 앱에 하드코딩하지 않고 이 값을 사용하면
 * 관리자가 설정을 바꿨을 때 앱 재배포 없이 반영된다. (SettingKey.clientVisible=true 인 항목만)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settings")
@Tag(name = "Setting", description = "공개 운영 설정 API")
public class PublicSettingController {

    private final AppSettingService appSettingService;

    @Operation(summary = "공개 운영 설정 조회",
            description = "앱 시작 시 호출해 클라이언트 측 입력 제한에 사용합니다. 응답 예: {\"POST_MAX_IMAGES\": \"5\", \"CHAT_MAX_TEXT_LENGTH\": \"1000\"}")
    @GetMapping("/public")
    public ApiResponse<Map<String, String>> getPublicSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        Arrays.stream(SettingKey.values())
                .filter(SettingKey::isClientVisible)
                .forEach(key -> result.put(key.name(), appSettingService.getString(key)));
        return ApiResponse.success(result);
    }
}
