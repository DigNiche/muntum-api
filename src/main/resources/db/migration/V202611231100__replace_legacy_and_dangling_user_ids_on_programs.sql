-- 프로그램·프로그램 이미지·프로그램 키워드의 사용자 칸(created_by, updated_by, deleted_by)을
-- "프로그램은 관리자 또는 시스템만 만들고 고치고 지운다"는 규칙에 맞게 정리한다.
--
-- 규칙
--   프로그램 등록·수정·이미지 수정·삭제·상태 변경 API는 관리자만 호출할 수 있다(@PreAuthorize MANAGER).
--   시스템(9999…)은 운영 종료 스케줄러가 상태를 바꿀 때 수정자로 들어간다.
--   그래서 탈퇴 처리는 관리자 탈퇴 때만 이 세 테이블의 사용자 칸을 탈퇴자 UUID로 바꾼다.
--
-- 1. 레거시: 큐레이터가 만든 프로그램 (탈퇴 처리가 아니다)
--   큐레이션과 프로그램이 분리되기 전, 큐레이터 한 명(014d9cea-…)이 큐레이션을 프로그램 형태로 작성했다.
--   이 회원은 지금도 활동 중인 큐레이터이고 탈퇴하지 않았다. 회원·역할·큐레이션은 건드리지 않는다.
--   위 규칙에 맞추고, 큐레이터 탈퇴 때 프로그램 칸을 바꾸지 않아도 되도록,
--   이 회원이 들어간 칸만 관리자(cf48de50-…)로 바꾼다(운영팀 결정. 기록상 이 관리자가 만든 것이 된다).
--   운영 기준 대상: programs 6칸(생성자 5, 수정자 1), program_images 4행, program_keywords 3행.
--   해당 관리자가 MANAGER로 있을 때만 바꾼다(없으면 사라진 회원 값을 새로 넣게 되므로 아무것도 하지 않는다).
--   로컬·테스트 DB에는 이 회원이 없어 바뀌는 행이 없다.
--
-- 2. 흔적: 처리가 빠진 채 탈퇴한 관리자
--   이전 탈퇴 처리는 프로그램 이미지·키워드의 사용자 칸을 바꾸지 않았다.
--   users에 없고 탈퇴자·시스템 UUID(8888…/7777…/6666…/9999…)도 아닌 값을 탈퇴 관리자 UUID로 바꾼다
--   (AuditorAwareImpl.toWithdrawnUserUuid와 같은 규칙: 8888… + 원래 UUID 끝 12자리).
--   이 테이블을 다루는 사람은 관리자뿐이므로 관리자로 본다.
--   운영 기준 대상: program_images.created_by 4행.
--
-- 되돌리기
--   1은 바꾼 행을 따로 기록하지 않으므로 되돌릴 수 없다. 2는 원래 UUID 앞부분이 사라져 되돌릴 수 없다.


-- 1. 레거시 큐레이터 → 관리자

SET @legacy_curator = UUID_TO_BIN('014d9cea-fd6b-44a3-8d74-d96ac68c00d0');
SET @assigned_manager = (
    SELECT u.`id` FROM `users` u
    WHERE u.`id` = UUID_TO_BIN('cf48de50-db8d-4d84-b524-3783f2c2d8b5') AND u.`role` = 'MANAGER');

UPDATE `programs` SET `created_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `created_by` = @legacy_curator;
UPDATE `programs` SET `updated_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `updated_by` = @legacy_curator;
UPDATE `programs` SET `deleted_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `deleted_by` = @legacy_curator;

UPDATE `program_images` SET `created_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `created_by` = @legacy_curator;
UPDATE `program_images` SET `updated_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `updated_by` = @legacy_curator;
UPDATE `program_images` SET `deleted_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `deleted_by` = @legacy_curator;

UPDATE `program_keywords` SET `created_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `created_by` = @legacy_curator;
UPDATE `program_keywords` SET `updated_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `updated_by` = @legacy_curator;
UPDATE `program_keywords` SET `deleted_by` = @assigned_manager WHERE @assigned_manager IS NOT NULL AND `deleted_by` = @legacy_curator;


-- 2. 사라진 회원 → 탈퇴 관리자 UUID

UPDATE `programs` t
SET t.`created_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`created_by`), 12)))
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`created_by`)
  AND LEFT(BIN_TO_UUID(t.`created_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `programs` t
SET t.`updated_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`updated_by`), 12)))
WHERE t.`updated_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`updated_by`)
  AND LEFT(BIN_TO_UUID(t.`updated_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `programs` t
SET t.`deleted_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`deleted_by`), 12)))
WHERE t.`deleted_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`deleted_by`)
  AND LEFT(BIN_TO_UUID(t.`deleted_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `program_images` t
SET t.`created_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`created_by`), 12)))
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`created_by`)
  AND LEFT(BIN_TO_UUID(t.`created_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `program_images` t
SET t.`updated_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`updated_by`), 12)))
WHERE t.`updated_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`updated_by`)
  AND LEFT(BIN_TO_UUID(t.`updated_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `program_images` t
SET t.`deleted_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`deleted_by`), 12)))
WHERE t.`deleted_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`deleted_by`)
  AND LEFT(BIN_TO_UUID(t.`deleted_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `program_keywords` t
SET t.`created_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`created_by`), 12)))
WHERE NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`created_by`)
  AND LEFT(BIN_TO_UUID(t.`created_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `program_keywords` t
SET t.`updated_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`updated_by`), 12)))
WHERE t.`updated_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`updated_by`)
  AND LEFT(BIN_TO_UUID(t.`updated_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');

UPDATE `program_keywords` t
SET t.`deleted_by` = UUID_TO_BIN(CONCAT('88888888-8888-8888-8888-', RIGHT(BIN_TO_UUID(t.`deleted_by`), 12)))
WHERE t.`deleted_by` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `users` u WHERE u.`id` = t.`deleted_by`)
  AND LEFT(BIN_TO_UUID(t.`deleted_by`), 24) NOT IN (
      '88888888-8888-8888-8888-', '77777777-7777-7777-7777-',
      '66666666-6666-6666-6666-', '99999999-9999-9999-9999-');
