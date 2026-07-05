-- enum 연동 코드(ROLE, BADGE, CATEGORY, ANIMAL, STATUS, GENDER, VISIBILITY,
-- LOG_ACTION, CHAT_MSG_TYPE, IMG_STATUS, UPLOAD_TARGET)는
-- CommonCodeSyncer가 앱 시작 시 자동으로 upsert하므로 여기서 관리하지 않는다.

-- 시스템 설정 (CONFIG) — enum이 아닌 운영 설정값만 관리
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CONFIG', 'MAX_POST_IMAGES', 'config.post.max_images', '5', true, NOW(), NOW());

-- 탐색 반경 설정 (SEARCH_RADIUS) — ref_1: 반경(미터). 운영 중 DB에서 ref_1 수정 가능
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES
    ('SEARCH_RADIUS', 'SMALL',  'search.radius.small',  '500',  true, NOW(), NOW()),
    ('SEARCH_RADIUS', 'MEDIUM', 'search.radius.medium', '1500', true, NOW(), NOW()),
    ('SEARCH_RADIUS', 'LARGE',  'search.radius.large',  '3000', true, NOW(), NOW());
