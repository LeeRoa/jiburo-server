package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminI18nDtos.MessageRow;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.SearchCondition;
import com.jiburo.server.domain.admin.dto.AdminI18nDtos.UpdateRequest;
import com.jiburo.server.domain.admin.service.AdminI18nService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/i18n")
@Tag(name = "Admin-I18n", description = "[관리자] 다국어 문구 관리 API")
public class AdminI18nController {

    private final AdminI18nService adminI18nService;

    @Operation(summary = "[관리자] 언어 목록", description = "파일 또는 DB에 문구가 있는 언어 (기준 언어 ko 먼저)")
    @GetMapping("/locales")
    @PreAuthorize("@adminAccess.read('I18N')")
    public ApiResponse<List<String>> locales() {
        return ApiResponse.success(adminI18nService.locales());
    }

    @Operation(summary = "[관리자] 문구 검색",
            description = "키 이름순. 각 행은 모든 언어의 현재 값·출처(DB/FILE/NONE)·기본값을 담습니다. "
                    + "status=MISSING으로 번역 누락, OVERRIDDEN으로 관리자 수정 키만 볼 수 있습니다.")
    @GetMapping("/messages")
    @PreAuthorize("@adminAccess.read('I18N')")
    public ApiResponse<Page<MessageRow>> search(
            @ParameterObject @ModelAttribute SearchCondition condition,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        return ApiResponse.success(adminI18nService.search(condition, pageable));
    }

    @Operation(summary = "[관리자] 문구 상세")
    @GetMapping("/messages/{key}")
    @PreAuthorize("@adminAccess.read('I18N')")
    public ApiResponse<MessageRow> find(@PathVariable String key) {
        return ApiResponse.success(adminI18nService.find(key));
    }

    @Operation(summary = "[관리자] 문구 수정/추가",
            description = "properties 기본값보다 우선 적용됩니다. 없는 키·언어를 지정하면 새로 추가됩니다. "
                    + "이 서버는 즉시, 다른 서버는 최대 60초 안에 반영됩니다.")
    @PutMapping("/messages/{key}/{locale}")
    @PreAuthorize("@adminAccess.write('I18N')")
    @AuditLog(action = LogActionType.ADMIN_I18N_UPDATE)
    public ApiResponse<MessageRow> update(
            @AuthenticationPrincipal CustomOAuth2User admin,
            @PathVariable String key,
            @PathVariable String locale,
            @Valid @RequestBody UpdateRequest request
    ) {
        return ApiResponse.success(adminI18nService.update(key, locale, request.message(), admin.getUserId()));
    }

    @Operation(summary = "[관리자] 기본값으로 되돌리기", description = "관리자 수정값을 지우고 properties 기본값을 다시 사용합니다.")
    @DeleteMapping("/messages/{key}/{locale}")
    @PreAuthorize("@adminAccess.write('I18N')")
    @AuditLog(action = LogActionType.ADMIN_I18N_RESET)
    public ApiResponse<MessageRow> reset(@PathVariable String key, @PathVariable String locale) {
        return ApiResponse.success(adminI18nService.reset(key, locale));
    }
}
