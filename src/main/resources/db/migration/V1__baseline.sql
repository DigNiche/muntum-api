-- Flyway baseline (V1)
-- Source      : production database `muntum` @ muntum-api-mysql-rds (ap-northeast-2)
-- Captured    : 2026-09-01 via `mysqldump --no-data --routines --triggers --events` (schema only)
-- Adjustments : `DROP TABLE IF EXISTS` statements removed (baseline must never drop data)
-- Note        : `social_accounts` intentionally uses COLLATE utf8mb4_0900_ai_ci to mirror
--               production; every other table uses utf8mb4_general_ci. Do not "normalize" here.
-- Execution   : runs only on empty schemas (fresh local / CI). Production is baselined at V1
--               (baseline-on-migrate) and skips this file.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `contents` text COLLATE utf8mb4_general_ci NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keywords` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `categories` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('SITUATION','SUBJECT','THEME') COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKpekgolf79aog8amsef5h7awbw` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `program_images` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `display_order` int NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `program_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_program_images_program_order` (`program_id`,`display_order`),
  CONSTRAINT `FKp61a04ikc1meya65uo68uqkdv` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `program_keywords` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `keyword_id` binary(16) NOT NULL,
  `program_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_program_keywords_program_keyword` (`program_id`,`keyword_id`),
  KEY `idx_program_keywords_program_id` (`program_id`),
  KEY `idx_program_keywords_keyword_id` (`keyword_id`),
  CONSTRAINT `FKfxyu906gp78v2lwdu6g3afsil` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`),
  CONSTRAINT `FKiv4adp5j28w23ju4pukc3d4f2` FOREIGN KEY (`keyword_id`) REFERENCES `keywords` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `program_reactions` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `program_id` binary(16) NOT NULL,
  `reaction_type` varchar(10) COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_program_reaction_user_program` (`user_id`,`program_id`),
  KEY `idx_program_reaction_user_type_updated` (`user_id`,`reaction_type`,`updated_at`),
  KEY `idx_program_reaction_program_type` (`program_id`,`reaction_type`),
  CONSTRAINT `fk_program_reaction_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`),
  CONSTRAINT `fk_program_reaction_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `programs` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `curation` text COLLATE utf8mb4_general_ci NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `is_free` bit(1) NOT NULL,
  `inquiry_contact` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `official_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `operating_hours` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `operating_hours_meta` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `operating_period_meta` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `price` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `type` enum('CLASS_EXPERIENCE','EXHIBITION','FAIR','PERFORMANCE') COLLATE utf8mb4_general_ci NOT NULL,
  `is_reserved` bit(1) NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` enum('ACTIVE','DELETED','ENDED','HIDDEN') COLLATE utf8mb4_general_ci NOT NULL,
  `tagline` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `venue_meta` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `venue_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scraps` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `program_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scrap_user_program` (`user_id`,`program_id`),
  KEY `idx_scrap_user_id` (`user_id`),
  KEY `idx_scrap_program_id` (`program_id`),
  CONSTRAINT `FKeg5ickeya8ic03etn3dgm1fb7` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`),
  CONSTRAINT `FKqd3nh3tj8ru0ubnk54qckuh42` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `social_accounts` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `provider` enum('APPLE','KAKAO') NOT NULL,
  `provider_user_id` varchar(255) NOT NULL,
  `provider_email` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `provider_refresh_token` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_provider_subject` (`provider`,`provider_user_id`),
  KEY `idx_social_accounts_user_id` (`user_id`),
  CONSTRAINT `fk_social_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_suggestions` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `program_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `reason` text COLLATE utf8mb4_general_ci,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED','REVIEWING') COLLATE utf8mb4_general_ci NOT NULL,
  `informer` binary(16) DEFAULT NULL,
  `reviewed_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_suggestions_user_id` (`informer`),
  KEY `idx_suggestions_status` (`status`),
  KEY `idx_suggestions_created_at` (`created_at`),
  KEY `FKooh5fxxsri4yxmvu9x8wr31md` (`reviewed_by`),
  CONSTRAINT `FKg9lq7ip4ppw8sket8euwlgp2y` FOREIGN KEY (`informer`) REFERENCES `users` (`id`),
  CONSTRAINT `FKooh5fxxsri4yxmvu9x8wr31md` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `content` longtext COLLATE utf8mb4_general_ci NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `effective_at` datetime(6) NOT NULL,
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('LOCATION_TERMS','MARKETING_EMAIL','MARKETING_PUSH','PRIVACY_POLICY','TERMS_OF_SERVICE','THIRD_PARTY_OFFER') COLLATE utf8mb4_general_ci NOT NULL,
  `version` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_terms_type_version` (`type`,`version`),
  KEY `idx_terms_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_keywords` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `keyword_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_keywords_user_keyword` (`user_id`,`keyword_id`),
  KEY `idx_user_keywords_user_id` (`user_id`),
  KEY `idx_user_keywords_keyword_id` (`keyword_id`),
  CONSTRAINT `FKau7mn5y9jdm7koto4fi1m26kw` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKm22o1w6ddb0w6072xki863qt` FOREIGN KEY (`keyword_id`) REFERENCES `keywords` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_terms_agreements` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `location_terms_at` datetime(6) DEFAULT NULL,
  `marketing_email_at` datetime(6) DEFAULT NULL,
  `marketing_push_at` datetime(6) DEFAULT NULL,
  `privacy_policy_at` datetime(6) NOT NULL,
  `terms_of_service_at` datetime(6) NOT NULL,
  `third_party_offer_at` datetime(6) DEFAULT NULL,
  `version` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_terms_user_version` (`user_id`,`version`),
  KEY `idx_user_terms_user_id` (`user_id`),
  CONSTRAINT `FKm93o3x274potqruayuh7l7526` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` binary(16) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `updated_by` binary(16) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` binary(16) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `email_verified` bit(1) NOT NULL,
  `email_verified_at` datetime(6) DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `profile_image_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reactivated_at` datetime(6) DEFAULT NULL,
  `role` enum('AUDIENCE','CURATOR','MANAGER') COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('ACTIVE','DELETED','INACTIVE','PENDING','SUSPENDED') COLLATE utf8mb4_general_ci NOT NULL,
  `taste_selected` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
