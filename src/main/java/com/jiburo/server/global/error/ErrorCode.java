package com.jiburo.server.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // [1] Global (공통 에러)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.global.internal_server"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "error.global.invalid_input"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "error.global.method_not_allowed"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "error.global.invalid_type"),
    FEATURE_NOT_READY(HttpStatus.NOT_IMPLEMENTED, "error.global.feature.not_ready"),
    INVALID_IDENTIFIER(HttpStatus.BAD_REQUEST, "error.global.invalid_identifier"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.global.not_found"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "error.global.unsupported_media_type"),
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "error.setting.not_found"),
    INVALID_SETTING_VALUE(HttpStatus.BAD_REQUEST, "error.setting.invalid_value"),

    // [2] Auth & User (인증 및 사용자)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.auth.unauthorized"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.auth.access_denied"), // 관리자 페이지 접근 거부 등
    OAUTH_PROVIDER_FAILED(HttpStatus.BAD_GATEWAY, "error.auth.oauth_failed"),
    OAUTH_PROVIDER_INVALID(HttpStatus.BAD_GATEWAY, "error.auth.oauth_invalid"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.not_found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "error.user.duplicate_email"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "error.user.duplicate_nickname"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "error.token.invalid"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "error.token.expired"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "error.token.refresh_invalid"),
    INVALID_AUTH_CODE(HttpStatus.UNAUTHORIZED, "error.auth.invalid_code"),

    // [3] Animal & Map (유기동물 및 지도)
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "error.animal.not_found"),
    ANIMAL_STATUS_INVALID(HttpStatus.BAD_REQUEST, "error.animal.status_invalid"),
    REGION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "error.animal.region_not_supported"),

    // [4] Image Upload (사진 업로드)
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "error.file.size_exceeded"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.file.upload_failed"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "error.file.invalid_format"),
    INVALID_FILE_CODE(HttpStatus.BAD_REQUEST, "error.file.invalid_code"),
    STORAGE_CAPACITY_EXCEEDED(HttpStatus.INSUFFICIENT_STORAGE, "error.file.storage_capacity_exceeded"),

    // [5] Chat & Community
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "error.chat.room_not_found"),
    ALREADY_JOINED_ROOM(HttpStatus.CONFLICT, "error.chat.already_joined"),
    CANNOT_CHAT_WITH_SELF(HttpStatus.BAD_REQUEST, "error.chat.cannot_with_self"),
    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.badge.not_found"),
    NOT_CHAT_PARTICIPANT(HttpStatus.FORBIDDEN, "error.chat.not_chat_participant"),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.chat.partner_not_found"),
    EMOTICON_NOT_FOUND(HttpStatus.NOT_FOUND, "error.emoticon.not_found"),
    EMOTICON_PACK_NOT_FOUND(HttpStatus.NOT_FOUND, "error.emoticon.pack_not_found"),
    INVALID_EMOTICON_IMAGE(HttpStatus.BAD_REQUEST, "error.emoticon.invalid_image"),
    INVALID_EMOTICON_ORDER(HttpStatus.BAD_REQUEST, "error.emoticon.invalid_order"),

    // [6] Post (게시글 - 분실물/목격 제보) [추가된 부분]
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "error.post.not_found"), // 게시글 없음
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.post.access_denied"), // 작성자 불일치 (수정/삭제 권한 없음)
    POST_CATEGORY_INVALID(HttpStatus.BAD_REQUEST, "error.post.category_invalid"),
    POST_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "error.post.image_limit_exceeded"),

    // [7] Finder Session (파인더 탐색 세션)
    FINDER_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "error.finder.session_not_found"),
    FINDER_SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.finder.session_access_denied"),
    FINDER_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.finder.session_already_exists"),
    FINDER_SESSION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "error.finder.session_already_submitted"),
    FINDER_POST_ALREADY_COMPLETE(HttpStatus.BAD_REQUEST, "error.finder.post_already_complete"),
    FINDER_CANNOT_FIND_OWN_POST(HttpStatus.BAD_REQUEST, "error.finder.cannot_find_own_post"),

    // [8] Admin (관리자 역할·권한)
    ADMIN_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.admin.role_not_found"),
    ADMIN_ROLE_CODE_DUPLICATE(HttpStatus.CONFLICT, "error.admin.role_code_duplicate"),
    ADMIN_ROLE_CODE_INVALID(HttpStatus.BAD_REQUEST, "error.admin.role_code_invalid"),
    ADMIN_SYSTEM_ROLE_IMMUTABLE(HttpStatus.BAD_REQUEST, "error.admin.system_role_immutable"),
    ADMIN_MENU_NOT_GRANTABLE(HttpStatus.BAD_REQUEST, "error.admin.menu_not_grantable"),
    ADMIN_CANNOT_CHANGE_OWN_ROLES(HttpStatus.BAD_REQUEST, "error.admin.cannot_change_own_roles"),
    ADMIN_SUPER_ADMIN_ONLY(HttpStatus.FORBIDDEN, "error.admin.super_admin_only"),
    ADMIN_ROLE_EXCEEDS_OWN_PERMISSION(HttpStatus.FORBIDDEN, "error.admin.role_exceeds_own_permission"),
    ADMIN_LAST_SUPER_ADMIN(HttpStatus.CONFLICT, "error.admin.last_super_admin");

    private final HttpStatus status;
    private final String messageKey;
}
