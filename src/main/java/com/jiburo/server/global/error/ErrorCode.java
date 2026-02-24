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

    // [3] Animal & Map (유기동물 및 지도)
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "error.animal.not_found"),
    ANIMAL_STATUS_INVALID(HttpStatus.BAD_REQUEST, "error.animal.status_invalid"),
    REGION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "error.animal.region_not_supported"),

    // [4] Image Upload (사진 업로드)
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "error.file.size_exceeded"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.file.upload_failed"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "error.file.invalid_format"),

    // [5] Chat & Community
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "error.chat.room_not_found"),
    ALREADY_JOINED_ROOM(HttpStatus.CONFLICT, "error.chat.already_joined"),
    CANNOT_CHAT_WITH_SELF(HttpStatus.BAD_REQUEST, "error.chat.cannot_with_self"),
    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.badge.not_found"),
    NOT_CHAT_PARTICIPANT(HttpStatus.FORBIDDEN, "error.chat.not_chat_participant"),

    // [6] Post (게시글 - 분실물/목격 제보) [추가된 부분]
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "error.post.not_found"), // 게시글 없음
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.post.access_denied"), // 작성자 불일치 (수정/삭제 권한 없음)
    POST_CATEGORY_INVALID(HttpStatus.BAD_REQUEST, "error.post.category_invalid");

    private final HttpStatus status;
    private final String messageKey;
}
