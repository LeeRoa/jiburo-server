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
    AUTH_LOGOUT("AUTH_LOGOUT", "log.auth.logout"),

    REPORT_CREATE("REPORT_CREATE", "log.report.create"),
    BADGE_UPGRADE("BADGE_UPGRADE", "log.badge.upgrade"),

    CHAT_ROOM_CREATE("CHAT_ROOM_CREATE", "log.chat.room.create"),
    CHAT_ROOM_EXIT("CHAT_ROOM_EXIT", "log.chat.room.exit"),

    FINDER_SESSION_CREATE("FINDER_SESSION_CREATE", "log.finder.session.create"),
    FINDER_SESSION_SUBMIT("FINDER_SESSION_SUBMIT", "log.finder.session.submit"),
    FINDER_SESSION_CANCEL("FINDER_SESSION_CANCEL", "log.finder.session.cancel"),

    // 관리자 작업 (관리자 API는 모두 ADMIN_ 접두사로 기록)
    ADMIN_COMMON_CODE_RELOAD("ADMIN_COMMON_CODE_RELOAD", "log.admin.common_code.reload"),
    ADMIN_SETTING_UPDATE("ADMIN_SETTING_UPDATE", "log.admin.setting.update"),
    ADMIN_SETTING_RESET("ADMIN_SETTING_RESET", "log.admin.setting.reset"),
    ADMIN_ROLE_CREATE("ADMIN_ROLE_CREATE", "log.admin.role.create"),
    ADMIN_ROLE_UPDATE("ADMIN_ROLE_UPDATE", "log.admin.role.update"),
    ADMIN_ROLE_MENU_UPDATE("ADMIN_ROLE_MENU_UPDATE", "log.admin.role.menu_update"),
    ADMIN_ROLE_DELETE("ADMIN_ROLE_DELETE", "log.admin.role.delete"),
    ADMIN_USER_ROLE_CHANGE("ADMIN_USER_ROLE_CHANGE", "log.admin.user.role_change"),
    ADMIN_POST_DELETE("ADMIN_POST_DELETE", "log.admin.post.delete"),
    ADMIN_POST_RESTORE("ADMIN_POST_RESTORE", "log.admin.post.restore"),

    SPARE("SPARE", "log.spare");

    private final String code;
    private final String descriptionKey;
    @Override public String getGroupCode() { return "LOG_ACTION"; }
}