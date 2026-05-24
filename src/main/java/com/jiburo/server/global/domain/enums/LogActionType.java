package com.jiburo.server.global.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LogActionType implements CommonCodeType {
    POST_CREATE("POST_CREATE", "log.post.create"),
    POST_UPDATE("POST_UPDATE", "log.post.update"),
    POST_DELETE("POST_DELETE", "log.post.delete"),
    POST_STATUS_CHANGE("POST_STATUS_CHANGE", "log.post.status.change"),

    USER_JOIN("USER_JOIN", "log.user.join"),
    USER_LOGIN("USER_LOGIN", "log.user.login"),
    USER_UPDATE("USER_UPDATE", "log.user.update"),
    USER_WITHDRAW("USER_WITHDRAW", "log.user.withdraw"),

    AUTH_LOGIN("AUTH_LOGIN", "log.auth.login"),
    AUTH_REISSUE("AUTH_REISSUE", "log.auth.reissue"),

    REPORT_CREATE("REPORT_CREATE", "log.report.create"),
    BADGE_UPGRADE("BADGE_UPGRADE", "log.badge.upgrade"),

    CHAT_ROOM_CREATE("CHAT_ROOM_CREATE", "log.chat.room.create"),
    CHAT_ROOM_EXIT("CHAT_ROOM_EXIT", "log.chat.room.exit"),

    SPARE("SPARE", "log.spare");

    private final String code;
    private final String descriptionKey;
    @Override public String getGroupCode() { return "LOG_ACTION"; }
}