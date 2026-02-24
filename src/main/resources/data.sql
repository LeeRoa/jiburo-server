-- 1. 권한 (ROLE)
-- CodeConst.Role 매핑
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ROLE', 'USER', 'role.user', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ROLE', 'ADMIN', 'role.admin', null, true, NOW(), NOW());


-- 2. 뱃지 등급 (BADGE)
-- CodeConst.Badge 매핑 (ref_1은 승급 기준 점수)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'BEGINNER', 'badge.beginner.title', '0', true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'JUNIOR', 'badge.junior.title', '30', true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'SENIOR', 'badge.senior.title', '100', true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'MASTER', 'badge.master.title', '300', true, NOW(), NOW());


-- 3. 게시글 대분류 (CATEGORY) - [NEW]
-- CodeConst.PostCategory 매핑
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CATEGORY', 'ANIMAL', 'category.animal', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CATEGORY', 'PERSON', 'category.person', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CATEGORY', 'ITEM', 'category.item', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CATEGORY', 'ETC', 'category.etc', null, true, NOW(), NOW());


-- 4. 동물 종류 (ANIMAL)
-- CodeConst.Animal 매핑 (CATEGORY가 'ANIMAL'일 때 사용되는 서브 코드)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ANIMAL', 'DOG', 'post.animal.dog', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ANIMAL', 'CAT', 'post.animal.cat', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ANIMAL', 'ETC', 'post.animal.etc', null, true, NOW(), NOW());


-- 5. 게시글 상태 (STATUS)
-- CodeConst.Status 매핑
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('STATUS', 'LOST', 'post.status.lost', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('STATUS', 'PROTECTING', 'post.status.protecting', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('STATUS', 'COMPLETE', 'post.status.complete', null, true, NOW(), NOW());


-- 6. 성별 (GENDER)
-- CodeConst.Gender (없지만 Post Entity에 사용되므로 유지)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('GENDER', 'MALE', 'post.gender.male', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('GENDER', 'FEMALE', 'post.gender.female', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('GENDER', 'UNKNOWN', 'post.gender.unknown', null, true, NOW(), NOW());


-- 7. 로그 액션 유형 (LOG_ACTION)
-- CodeConst.LogAction 매핑 (관리자 페이지 필터용)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'POST_CREATE', 'log.post.create', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'POST_UPDATE', 'log.post.update', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'POST_DELETE', 'log.post.delete', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'POST_STATUS_CHANGE', 'log.post.status_change', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'USER_JOIN', 'log.user.join', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'USER_LOGIN', 'log.user.login', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'USER_UPDATE', 'log.user.update', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'USER_WITHDRAW', 'log.user.withdraw', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'AUTH_LOGIN', 'log.auth.login', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'AUTH_REISSUE', 'log.auth.reissue', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'AUTH_LOGOUT', 'log.auth.logout', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'REPORT_CREATE', 'log.report.create', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('LOG_ACTION', 'BADGE_UPGRADE', 'log.badge.upgrade', null, true, NOW(), NOW());

-- 8. 시스템 설정 (CONFIG)
-- 시스템 전반적인 설정값을 관리 (ref_1 컬럼에 실제 설정값 저장)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('CONFIG', 'MAX_POST_IMAGES', 'config.post.max_images', '5', true, NOW(), NOW());

-- 9. 게시글 공개 상태 (VISIBILITY)
-- CodeConst.Visibility 매핑 (공개 권한 제어용)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('VISIBILITY', 'PUBLIC', 'post.visibility.public', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('VISIBILITY', 'PROTECTED', 'post.visibility.protected', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at) VALUES ('VISIBILITY', 'PRIVATE', 'post.visibility.private', null, true, NOW(), NOW());

-- 10. 채팅 메시지 유형 (CHAT_MSG_TYPE)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CHAT_MSG_TYPE', 'TALK', 'chat.msg.talk', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CHAT_MSG_TYPE', 'IMAGE', 'chat.msg.image', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CHAT_MSG_TYPE', 'VIDEO', 'chat.msg.video', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CHAT_MSG_TYPE', 'MAP', 'chat.msg.map', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('CHAT_MSG_TYPE', 'SYS', 'chat.msg.sys', null, true, NOW(), NOW());

-- 11. 알림 유형 (NOTI_TYPE)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('NOTI_TYPE', 'COMMENT', 'noti.type.comment', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('NOTI_TYPE', 'CHAT', 'noti.type.chat', null, true, NOW(), NOW());
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('NOTI_TYPE', 'SYSTEM', 'noti.type.system', null, true, NOW(), NOW());
