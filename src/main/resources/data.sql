-- enum 연동 코드(ROLE, BADGE, CATEGORY, ANIMAL, STATUS, GENDER, VISIBILITY,
-- LOG_ACTION, CHAT_MSG_TYPE, IMG_STATUS, UPLOAD_TARGET)는
-- CommonCodeSyncer가 앱 시작 시 자동으로 upsert하므로 여기서 관리하지 않는다.

-- 시스템 설정 (CONFIG) — enum이 아닌 운영 설정값만 관리
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CONFIG', 'MAX_POST_IMAGES', 'config.post.max_images', '5', true, NOW(), NOW());
