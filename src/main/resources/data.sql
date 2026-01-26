-- 1. 권한 (ROLE)
-- 이미 'ROLE' 그룹에 'USER' 코드가 있다면 무시하고 넘어갑니다.
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ROLE', 'USER', 'role.user', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ROLE', 'ADMIN', 'role.admin', null, true, NOW(), NOW());


-- 2. 뱃지 등급 (BADGE)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'BEGINNER', 'badge.beginner.title', '0', true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'JUNIOR', 'badge.junior.title', '30', true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'SENIOR', 'badge.senior.title', '100', true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('BADGE', 'MASTER', 'badge.master.title', '300', true, NOW(), NOW());


-- 3. 동물 종류 (ANIMAL)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ANIMAL', 'DOG', 'post.animal.dog', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ANIMAL', 'CAT', 'post.animal.cat', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('ANIMAL', 'ETC', 'post.animal.etc', null, true, NOW(), NOW());


-- 4. 게시글 상태 (STATUS)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('STATUS', 'LOST', 'post.status.lost', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('STATUS', 'PROTECTING', 'post.status.protecting', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('STATUS', 'COMPLETE', 'post.status.complete', null, true, NOW(), NOW());


-- 5. 성별 (GENDER)
INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('GENDER', 'MALE', 'post.gender.male', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('GENDER', 'FEMALE', 'post.gender.female', null, true, NOW(), NOW());

INSERT IGNORE INTO common_codes (code_group, code, message_key, ref_1, use_yn, created_at, updated_at)
VALUES ('GENDER', 'UNKNOWN', 'post.gender.unknown', null, true, NOW(), NOW());