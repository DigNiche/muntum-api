-- 큐레이션·큐레이션 이미지의 사용자 칸 중, 사라진 회원을 가리키는 값을 탈퇴자 UUID로 바꾼다.
--
-- 이유
--   이전 탈퇴 처리는 큐레이션의 작성자 칸을 바꾸지 않고 회원 행만 지웠다.
--   그래서 탈퇴한 큐레이터의 curator_id가 users에 없는 값으로 남았고, 큐레이터 표시에서 USER_NOT_FOUND가 나
--   그 큐레이션이 붙은 프로그램 상세가 실패한다. 이제 탈퇴 처리가 이 칸들을 바꾸므로, 이미 남은 값만 정리한다.
--
-- 대상
--   "사라진 회원" = users에 없고, 탈퇴자·시스템 UUID(8888…/7777…/6666…/9999…)도 아닌 값
--   curations: curator_id, created_by, updated_by, reviewed_by, deleted_by
--   curation_images: created_by, updated_by
--
-- 바꿀 값 (AuditorAwareImpl.toWithdrawnUserUuid와 같은 규칙: 역할 접두사 + 원래 UUID 끝 12자리)
--   회원이 지워져 역할을 알 수 없으므로 이렇게 나눈다.
--   - 사라진 curator_id로 쓰인 값 → 큐레이터(7777…)
--   - 그 밖의 값 → 관리자(8888…). 남의 큐레이션을 검토·수정·삭제하는 사람은 관리자뿐이다
--   한 사람은 모든 칸에서 같은 값으로 바뀐다.
--
-- 되돌리기
--   원래 UUID의 앞부분이 사라지므로 되돌릴 수 없다.

CREATE TEMPORARY TABLE `tmp_withdrawn_curator_ids` (
    `id` binary(16) NOT NULL PRIMARY KEY
);

INSERT INTO `tmp_withdrawn_curator_ids` (`id`)
SELECT DISTINCT c.`curator_id`
FROM `curations` c
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = c.`curator_id`)
  AND LEFT(BIN_TO_UUID(c.`curator_id`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');


-- curations (curator_id는 분류에 쓰이므로 맨 마지막에 바꾼다)
UPDATE `curations` c
SET c.`created_by` = UUID_TO_BIN(CONCAT(
        IF(EXISTS (SELECT 1 FROM `tmp_withdrawn_curator_ids` t WHERE t.`id` = c.`created_by`),
           '77777777-7777-7777-7777-', '88888888-8888-8888-8888-'),
        RIGHT(BIN_TO_UUID(c.`created_by`), 12)))
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = c.`created_by`)
  AND LEFT(BIN_TO_UUID(c.`created_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `curations` c
SET c.`updated_by` = UUID_TO_BIN(CONCAT(
        IF(EXISTS (SELECT 1 FROM `tmp_withdrawn_curator_ids` t WHERE t.`id` = c.`updated_by`),
           '77777777-7777-7777-7777-', '88888888-8888-8888-8888-'),
        RIGHT(BIN_TO_UUID(c.`updated_by`), 12)))
WHERE c.`updated_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = c.`updated_by`)
  AND LEFT(BIN_TO_UUID(c.`updated_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `curations` c
SET c.`reviewed_by` = UUID_TO_BIN(CONCAT(
        IF(EXISTS (SELECT 1 FROM `tmp_withdrawn_curator_ids` t WHERE t.`id` = c.`reviewed_by`),
           '77777777-7777-7777-7777-', '88888888-8888-8888-8888-'),
        RIGHT(BIN_TO_UUID(c.`reviewed_by`), 12)))
WHERE c.`reviewed_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = c.`reviewed_by`)
  AND LEFT(BIN_TO_UUID(c.`reviewed_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `curations` c
SET c.`deleted_by` = UUID_TO_BIN(CONCAT(
        IF(EXISTS (SELECT 1 FROM `tmp_withdrawn_curator_ids` t WHERE t.`id` = c.`deleted_by`),
           '77777777-7777-7777-7777-', '88888888-8888-8888-8888-'),
        RIGHT(BIN_TO_UUID(c.`deleted_by`), 12)))
WHERE c.`deleted_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = c.`deleted_by`)
  AND LEFT(BIN_TO_UUID(c.`deleted_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');


-- curation_images
UPDATE `curation_images` i
SET i.`created_by` = UUID_TO_BIN(CONCAT(
        IF(EXISTS (SELECT 1 FROM `tmp_withdrawn_curator_ids` t WHERE t.`id` = i.`created_by`),
           '77777777-7777-7777-7777-', '88888888-8888-8888-8888-'),
        RIGHT(BIN_TO_UUID(i.`created_by`), 12)))
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = i.`created_by`)
  AND LEFT(BIN_TO_UUID(i.`created_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `curation_images` i
SET i.`updated_by` = UUID_TO_BIN(CONCAT(
        IF(EXISTS (SELECT 1 FROM `tmp_withdrawn_curator_ids` t WHERE t.`id` = i.`updated_by`),
           '77777777-7777-7777-7777-', '88888888-8888-8888-8888-'),
        RIGHT(BIN_TO_UUID(i.`updated_by`), 12)))
WHERE i.`updated_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = i.`updated_by`)
  AND LEFT(BIN_TO_UUID(i.`updated_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');


-- curator_id (사라진 작성자는 모두 큐레이터로 본다)
UPDATE `curations` c
SET c.`curator_id` = UUID_TO_BIN(CONCAT('77777777-7777-7777-7777-', RIGHT(BIN_TO_UUID(c.`curator_id`), 12)))
WHERE c.`curator_id` IN (SELECT t.`id` FROM `tmp_withdrawn_curator_ids` t);

DROP TEMPORARY TABLE `tmp_withdrawn_curator_ids`;
