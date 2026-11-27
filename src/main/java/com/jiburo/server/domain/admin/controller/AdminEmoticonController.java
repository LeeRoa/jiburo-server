package com.jiburo.server.domain.admin.controller;

import com.jiburo.server.domain.admin.dto.AdminEmoticonDtos.*;
import com.jiburo.server.domain.admin.service.AdminEmoticonService;
import com.jiburo.server.domain.image.dto.PresignedUrlResponseDto;
import com.jiburo.server.domain.user.dto.CustomOAuth2User;
import com.jiburo.server.global.domain.enums.LogActionType;
import com.jiburo.server.global.log.annotation.AuditLog;
import com.jiburo.server.global.response.ApiResponse;
import com.jiburo.server.global.util.HashidsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AdminApi.BASE_PATH + "/emoticons")
@Tag(name = "Admin-Emoticon", description = "[관리자] 채팅 이모티콘 관리 API")
public class AdminEmoticonController {

    private final AdminEmoticonService adminEmoticonService;

    @Operation(summary = "[관리자] 이모티콘 묶음 전체 조회", description = "비노출 묶음 포함, 정렬 순서대로")
    @GetMapping("/packs")
    @PreAuthorize("@adminAccess.read('EMOTICON')")
    public ApiResponse<List<PackResponse>> findAll() {
        return ApiResponse.success(adminEmoticonService.findAll());
    }

    @Operation(summary = "[관리자] 이모티콘 묶음 생성", description = "목록 맨 뒤에 추가. visible 기본 false")
    @PostMapping("/packs")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_PACK_CREATE)
    public ApiResponse<PackResponse> createPack(@Valid @RequestBody CreatePackRequest request) {
        return ApiResponse.success(adminEmoticonService.createPack(request));
    }

    @Operation(summary = "[관리자] 이모티콘 묶음 수정", description = "이름 / 노출 여부 (보낸 값만 변경)")
    @PatchMapping("/packs/{packId}")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_PACK_UPDATE)
    public ApiResponse<PackResponse> updatePack(@PathVariable String packId, @Valid @RequestBody UpdatePackRequest request) {
        return ApiResponse.success(adminEmoticonService.updatePack(HashidsUtils.decode(packId), request));
    }

    @Operation(summary = "[관리자] 이모티콘 묶음 삭제", description = "소속 이모티콘도 삭제. 이미 보낸 채팅 메시지는 그대로 보입니다.")
    @DeleteMapping("/packs/{packId}")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_PACK_DELETE)
    public ApiResponse<Void> deletePack(@PathVariable String packId) {
        adminEmoticonService.deletePack(HashidsUtils.decode(packId));
        return ApiResponse.success();
    }

    @Operation(summary = "[관리자] 이모티콘 묶음 순서 변경", description = "전체 묶음 ID를 새 순서대로 전달")
    @PutMapping("/packs/order")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_PACK_UPDATE)
    public ApiResponse<Void> reorderPacks(@Valid @RequestBody OrderRequest request) {
        adminEmoticonService.reorderPacks(request.ids());
        return ApiResponse.success();
    }

    @Operation(summary = "[관리자] 이모티콘 이미지 업로드 URL 발급",
            description = "png / gif / webp (움직이는 이모티콘 허용). R2에 PUT 후 /api/v1/images/complete 호출, "
                    + "그 다음 /packs/{packId}/emoticons로 등록")
    @PostMapping("/presigned-url")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    public ApiResponse<PresignedUrlResponseDto> presign(@AuthenticationPrincipal CustomOAuth2User user,
                                                        @Valid @RequestBody PresignRequest request) {
        return ApiResponse.success(adminEmoticonService.presign(user.getUserId(), request));
    }

    @Operation(summary = "[관리자] 묶음에 이모티콘 추가", description = "업로드 완료된 이미지 키들을 순서대로 묶음 끝에 추가")
    @PostMapping("/packs/{packId}/emoticons")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_ADD)
    public ApiResponse<PackResponse> addEmoticons(@PathVariable String packId, @Valid @RequestBody AddEmoticonsRequest request) {
        return ApiResponse.success(adminEmoticonService.addEmoticons(HashidsUtils.decode(packId), request.imageKeys()));
    }

    @Operation(summary = "[관리자] 묶음 안 이모티콘 순서 변경", description = "묶음의 전체 이모티콘 ID를 새 순서대로 전달")
    @PutMapping("/packs/{packId}/emoticons/order")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_PACK_UPDATE)
    public ApiResponse<Void> reorderEmoticons(@PathVariable String packId, @Valid @RequestBody OrderRequest request) {
        adminEmoticonService.reorderEmoticons(HashidsUtils.decode(packId), request.ids());
        return ApiResponse.success();
    }

    @Operation(summary = "[관리자] 이모티콘 삭제", description = "이미 보낸 채팅 메시지는 그대로 보입니다.")
    @DeleteMapping("/packs/{packId}/emoticons/{emoticonId}")
    @PreAuthorize("@adminAccess.write('EMOTICON')")
    @AuditLog(action = LogActionType.ADMIN_EMOTICON_DELETE)
    public ApiResponse<Void> deleteEmoticon(@PathVariable String packId, @PathVariable String emoticonId) {
        adminEmoticonService.deleteEmoticon(HashidsUtils.decode(packId), HashidsUtils.decode(emoticonId));
        return ApiResponse.success();
    }
}
