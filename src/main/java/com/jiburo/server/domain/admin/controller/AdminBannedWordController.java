package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.AddRequest;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.AddResult;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.TestResult;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.UpdateRequest;
import com.jiburo.server.domain.admin.dto.AdminBannedWordDtos.WordResponse;
import com.jiburo.server.domain.admin.service.AdminBannedWordService;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/banned-words")
@Tag(name = "Admin-BannedWord", description = "[관리자] 금지어 관리 API")
public class AdminBannedWordController {

    private final AdminBannedWordService adminBannedWordService;

    @Operation(summary = "[관리자] 금지어 검색", description = "부분 일치, 기본 최신 등록순")
    @GetMapping
    @PreAuthorize("@adminAccess.read('BANNED_WORD')")
    public ApiResponse<Page<WordResponse>> search(
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(adminBannedWordService.search(keyword, pageable));
    }

    @Operation(summary = "[관리자] 금지어 추가",
            description = "여러 개 한 번에 (최대 100개). 대소문자·공백은 무시하고 저장하며 이미 있는 단어는 건너뜁니다. "
                    + "채팅 텍스트·게시글 제목/내용은 ***로 가려지고, 닉네임에는 쓸 수 없습니다.")
    @PostMapping
    @PreAuthorize("@adminAccess.write('BANNED_WORD')")
    @AuditLog(action = LogActionType.ADMIN_BANNED_WORD_ADD)
    public ApiResponse<AddResult> add(@AuthenticationPrincipal CustomOAuth2User admin, @Valid @RequestBody AddRequest request) {
        return ApiResponse.success(adminBannedWordService.add(request.words(), admin.getUserId()));
    }

    @Operation(summary = "[관리자] 금지어 수정",
            description = "오타 정정 등. 대소문자·공백은 무시하고 저장하며, 다른 항목과 같은 단어로는 바꿀 수 없습니다 (BANNED_WORD_DUPLICATED). "
                    + "수정자·수정 시각이 응답에 담깁니다.")
    @PatchMapping("/{wordId}")
    @PreAuthorize("@adminAccess.write('BANNED_WORD')")
    @AuditLog(action = LogActionType.ADMIN_BANNED_WORD_UPDATE)
    public ApiResponse<WordResponse> update(@AuthenticationPrincipal CustomOAuth2User admin,
                                            @PathVariable String wordId,
                                            @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.success(adminBannedWordService.update(HashidsUtils.decode(wordId), request.word(), admin.getUserId()));
    }

    @Operation(summary = "[관리자] 금지어 삭제", description = "이미 가려져 저장된 글·메시지는 되돌아가지 않습니다.")
    @DeleteMapping("/{wordId}")
    @PreAuthorize("@adminAccess.write('BANNED_WORD')")
    @AuditLog(action = LogActionType.ADMIN_BANNED_WORD_DELETE)
    public ApiResponse<Void> delete(@PathVariable String wordId) {
        adminBannedWordService.delete(HashidsUtils.decode(wordId));
        return ApiResponse.success();
    }

    @Operation(summary = "[관리자] 금지어 적용 미리보기", description = "등록된 금지어로 문장을 가려 봅니다 (저장하지 않음).")
    @GetMapping("/test")
    @PreAuthorize("@adminAccess.read('BANNED_WORD')")
    public ApiResponse<TestResult> test(@RequestParam String text) {
        return ApiResponse.success(adminBannedWordService.test(text));
    }
}
