-- CuratorApplication 엔티티 (feat/curator/application)
CREATE TABLE `curator_applications` (
                                        `id` binary(16) NOT NULL,
                                        `created_at` datetime(6) NOT NULL,
                                        `created_by` binary(16) NOT NULL,
                                        `updated_at` datetime(6) DEFAULT NULL,
                                        `updated_by` binary(16) DEFAULT NULL,
                                        `applicant_id` binary(16) NOT NULL,
                                        `program_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
                                        `tagline` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
                                        `curation` text COLLATE utf8mb4_general_ci NOT NULL,
                                        `status` enum('APPROVED','PENDING','REJECTED') COLLATE utf8mb4_general_ci NOT NULL,
                                        `reject_reason` enum('GUIDELINE_MISMATCH','INSUFFICIENT_INFO','PROGRAM_MISMATCH') COLLATE utf8mb4_general_ci DEFAULT NULL,
                                        `reviewed_by` binary(16) DEFAULT NULL,
                                        `reviewed_at` datetime(6) DEFAULT NULL,
                                        PRIMARY KEY (`id`),
                                        KEY `idx_curator_applications_applicant_id` (`applicant_id`),
                                        KEY `idx_curator_applications_status` (`status`),
                                        KEY `idx_curator_applications_created_at` (`created_at`),
                                        CONSTRAINT `fk_curator_applications_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;