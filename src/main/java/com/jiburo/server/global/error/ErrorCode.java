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

    // [2] Auth & User (인증 및 사용자)
    // 401 Unauthorized: 인증되지 않음 (로그인 필요)
    // 403 Forbidden: 권한 없음 (내 글이 아님, 관리자 전용 등)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.auth.unauthorized"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.auth.access_denied"),
    OAUTH_PROVIDER_FAILED(HttpStatus.BAD_GATEWAY, "error.auth.oauth_failed"),
    OAUTH_PROVIDER_INVALID(HttpStatus.BAD_GATEWAY, "error.auth.oauth_invalid"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.not_found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "error.user.duplicate_email"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "error.user.duplicate_nickname"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "error.token.invalid"),      // 유효하지 않은 토큰
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "error.token.expired"),      // 만료된 토큰 (필요시 추가)
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "error.token.refresh_invalid"), // 리프레시 토큰 불일치

    // [3] Animal & Map (유기동물 및 지도)
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "error.animal.not_found"),
    ANIMAL_STATUS_INVALID(HttpStatus.BAD_REQUEST, "error.animal.status_invalid"), // 이미 찾은 동물 상태 변경 시 등
    REGION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "error.animal.region_not_supported"), // 서비스 지원 외 지역

    // [4] Image Upload (사진 업로드 - 모바일 웹 필수)
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "error.file.size_exceeded"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.file.upload_failed"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "error.file.invalid_format"),

    // [5] Chat & Community (채팅 및 커뮤니티 - 추후 고도화)
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "error.chat.room_not_found"),
    ALREADY_JOINED_ROOM(HttpStatus.CONFLICT, "error.chat.already_joined"),
    BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.badge.not_found");

    private final HttpStatus status;
    private final String messageKey; // messages.properties의 키값
}