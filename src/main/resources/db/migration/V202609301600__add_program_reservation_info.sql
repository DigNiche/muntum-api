-- =========================================================
-- Program reservation info
-- 1. Add programs.reservation_type (예약 방식)
-- 2. Add programs.reservation_url (예약 링크)
--
-- Compatibility:
-- - both columns are nullable (선택 입력)
-- - existing programs stay NULL; PM selects them in the app
-- - programs.is_reserved is intentionally retained
-- =========================================================

ALTER TABLE `programs`
    ADD COLUMN `reservation_type`
        enum('FREE_ENTRY','ON_SITE','PRE_REGISTRATION','PRE_REGISTRATION_AND_ON_SITE')
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_general_ci
        DEFAULT NULL
        AFTER `is_reserved`,
    ADD COLUMN `reservation_url` varchar(500)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_general_ci
        DEFAULT NULL
        AFTER `reservation_type`;
