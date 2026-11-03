package com.jiburo.server.global.setting;

import lombok.Getter;

import static com.jiburo.server.global.setting.SettingKey.Category.*;

/**
 * 관리자 웹에서 바꿀 수 있는 운영 설정 목록 (단일 진실 공급원).
 *
 * - 새 설정이 필요하면 여기에 한 줄 추가하고, 코드에서는 AppSettingService.getInt(SettingKey.XXX) 등으로 읽는다.
 * - DB(app_settings)에는 관리자가 바꾼 값만 저장되며, 없으면 defaultValue를 사용한다.
 * - min/max로 저장 전에 범위를 검증하므로 잘못된 값 때문에 서비스가 망가지지 않는다.
 * - clientVisible=true 인 설정은 비로그인 공개 API(/api/v1/settings/public)로 앱/웹에 내려간다
 *   (예: 이미지 최대 장수처럼 클라이언트가 미리 알아야 하는 제한값). 보안상 민감한 값은 false.
 *
 * JWT 서명키, DB/Redis 접속 정보, CORS처럼 기동/보안에 필요한 값은 여기 두지 않고 환경변수로 관리한다.
 */
@Getter
public enum SettingKey {

    // ── 업로드 ──
    POST_MAX_IMAGES(UPLOAD, SettingType.INT, "5", 1d, 20d, "장", true,
            "게시글 1개에 첨부할 수 있는 최대 이미지 수"),
    UPLOAD_MAX_IMAGE_SIZE_MB(UPLOAD, SettingType.INT, "10", 1d, 100d, "MB", true,
            "이미지 파일 1개의 최대 크기"),
    UPLOAD_MAX_VIDEO_SIZE_MB(UPLOAD, SettingType.INT, "200", 1d, 2048d, "MB", true,
            "동영상 파일 1개의 최대 크기"),
    UPLOAD_TOTAL_CAPACITY_GB(UPLOAD, SettingType.INT, "10", 1d, 100000d, "GB", false,
            "전체 업로드 저장 용량 한도 (초과 시 업로드 차단)"),
    UPLOAD_PRESIGNED_EXPIRATION_MINUTES(UPLOAD, SettingType.INT, "5", 1d, 60d, "분", false,
            "업로드용 presigned URL 유효 시간"),

    // ── 채팅 ──
    CHAT_MAX_TEXT_LENGTH(CHAT, SettingType.INT, "1000", 1d, 5000d, "자", true,
            "채팅 텍스트 메시지 최대 길이"),

    // ── 검색 / 지도 ──
    SEARCH_HISTORY_MAX(SEARCH, SettingType.INT, "10", 1d, 50d, "개", false,
            "사용자별 키워드/위치 검색 기록 보관 개수"),
    NEARBY_DEFAULT_RADIUS_KM(SEARCH, SettingType.DOUBLE, "3.0", 0.1d, 100d, "km", true,
            "반경 검색 시 radius를 보내지 않았을 때 사용할 기본 반경"),
    NEARBY_MAX_RADIUS_KM(SEARCH, SettingType.DOUBLE, "50.0", 0.1d, 500d, "km", true,
            "반경 검색 최대 반경 (초과 요청은 거부)"),
    NEARBY_MAX_PAGE_SIZE(SEARCH, SettingType.INT, "50", 1d, 200d, "개", false,
            "반경 검색 목록 1회 최대 조회 개수 (초과 요청은 이 값으로 제한)"),
    MAP_MAX_MARKERS(SEARCH, SettingType.INT, "300", 10d, 2000d, "개", false,
            "지도 화면 1회 최대 마커 수 (초과 요청은 이 값으로 제한)"),

    // ── 인증 ── (변경 시 이후 새로 발급되는 토큰부터 적용)
    ACCESS_TOKEN_TTL_MINUTES(AUTH, SettingType.INT, "30", 5d, 1440d, "분", false,
            "Access Token 유효 시간"),
    REFRESH_TOKEN_TTL_DAYS(AUTH, SettingType.INT, "14", 1d, 90d, "일", false,
            "Refresh Token 유효 시간"),
    LOGIN_CODE_TTL_SECONDS(AUTH, SettingType.INT, "60", 10d, 300d, "초", false,
            "웹 OAuth 로그인 1회용 code 유효 시간");

    public enum Category { UPLOAD, CHAT, SEARCH, AUTH, I18N }

    private final Category category;
    private final SettingType type;
    private final String defaultValue;
    private final Double min;
    private final Double max;
    private final String unit;
    private final boolean clientVisible;
    private final String description;

    SettingKey(Category category, SettingType type, String defaultValue, Double min, Double max,
               String unit, boolean clientVisible, String description) {
        this.category = category;
        this.type = type;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.clientVisible = clientVisible;
        this.description = description;
    }
}
