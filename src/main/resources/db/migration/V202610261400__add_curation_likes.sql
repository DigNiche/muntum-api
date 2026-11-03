-- =========================================================
-- Curation likes
-- 1. curation_likes: (user, curation) 사이의 좋아요 상태, 복합 PK
-- 2. curations.like_count: 좋아요 수 비정규화 (기존 큐레이션은 좋아요가 없으므로 0)
--
-- FK는 ON DELETE CASCADE 없이 둔다.
-- 관련 데이터는 코드에서 지우고, 정리를 빠뜨리면 FK 에러로 드러나게 한다.
-- =========================================================

CREATE TABLE `curation_likes` (
                                  `user_id` binary(16) NOT NULL,
                                  `curation_id` binary(16) NOT NULL,
                                  `created_at` datetime(6) NOT NULL,

                                  PRIMARY KEY (`user_id`, `curation_id`),

                                  KEY `idx_curation_likes_curation_created_user`
                                      (`curation_id`, `created_at`, `user_id`),

                                  CONSTRAINT `fk_curation_likes_user`
                                      FOREIGN KEY (`user_id`)
                                          REFERENCES `users` (`id`),

                                  CONSTRAINT `fk_curation_likes_curation`
                                      FOREIGN KEY (`curation_id`)
                                          REFERENCES `curations` (`id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;

ALTER TABLE `curations`
    ADD COLUMN `like_count` int NOT NULL DEFAULT 0;
