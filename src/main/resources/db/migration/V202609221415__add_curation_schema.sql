-- =========================================================
-- Curation schema
-- 1. Add Program.description for new general-program content
-- 2. Backfill legacy programs.curation -> description
-- 3. Create curations
-- 4. Create curation_images
--
-- Compatibility:
-- - programs.tagline and programs.curation are intentionally retained
-- - description stays nullable during the expand phase
-- - no legacy Program -> Curation data migration is required
--   because current production data has no Program created by CURATOR
-- =========================================================


-- ---------------------------------------------------------
-- 1. programs.description
-- ---------------------------------------------------------

ALTER TABLE `programs`
    ADD COLUMN `description` TEXT
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_general_ci
        DEFAULT NULL
        AFTER `curation`;


-- ---------------------------------------------------------
-- 2. Backfill existing Program data
-- ---------------------------------------------------------

UPDATE `programs`
SET `description` = `curation`
WHERE `description` IS NULL;


-- ---------------------------------------------------------
-- 3. curations
-- ---------------------------------------------------------

CREATE TABLE `curations` (
                             `id` binary(16) NOT NULL,

                             `created_at` datetime(6) NOT NULL,
                             `created_by` binary(16) NOT NULL,
                             `updated_at` datetime(6) DEFAULT NULL,
                             `updated_by` binary(16) DEFAULT NULL,

                             `program_id` binary(16) DEFAULT NULL,
                             `curator_id` binary(16) NOT NULL,

                             `submitted_program_title` varchar(100)
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                 NOT NULL,

                             `submitted_place` varchar(255)
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                 NOT NULL,

                             `tagline` varchar(255)
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                 NOT NULL,

                             `content` text
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                 NOT NULL,

                             `status` enum(
                                 'APPROVED',
                                 'CHANGES_REQUESTED',
                                 'PENDING'
                                 )
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                 NOT NULL,

                             `publication_status` enum(
                                 'PUBLISHED',
                                 'UNPUBLISHED'
                                 )
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                 NOT NULL,

                             `change_request_reason` varchar(1000)
                                      CHARACTER SET utf8mb4
                                      COLLATE utf8mb4_general_ci
                                                      DEFAULT NULL,

                             `reviewed_by` binary(16) DEFAULT NULL,
                             `reviewed_at` datetime(6) DEFAULT NULL,

                             PRIMARY KEY (`id`),

                             UNIQUE KEY `uk_program_curations_program_curator`
                                 (`program_id`, `curator_id`),

                             KEY `idx_program_curations_curator_created`
                                 (`curator_id`, `created_at`),

                             KEY `idx_program_curations_status_created`
                                 (`status`, `created_at`),

                             KEY `idx_program_curations_program_status`
                                 (`program_id`, `status`),

                             CONSTRAINT `fk_program_curations_program`
                                 FOREIGN KEY (`program_id`)
                                     REFERENCES `programs` (`id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;


-- ---------------------------------------------------------
-- 4. curation_images
-- ---------------------------------------------------------

CREATE TABLE `curation_images` (
                                   `id` binary(16) NOT NULL,

                                   `created_at` datetime(6) NOT NULL,
                                   `created_by` binary(16) NOT NULL,
                                   `updated_at` datetime(6) DEFAULT NULL,
                                   `updated_by` binary(16) DEFAULT NULL,

                                   `program_curation_id` binary(16) NOT NULL,

                                   `image_url` varchar(500)
                                            CHARACTER SET utf8mb4
                                            COLLATE utf8mb4_general_ci
                                       NOT NULL,

                                   `display_order` int NOT NULL,

                                   PRIMARY KEY (`id`),

                                   UNIQUE KEY `uk_program_curation_images_order`
                                       (`program_curation_id`, `display_order`),

                                   KEY `idx_program_curation_images_curation`
                                       (`program_curation_id`),

                                   CONSTRAINT `fk_program_curation_images_curation`
                                       FOREIGN KEY (`program_curation_id`)
                                           REFERENCES `curations` (`id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;