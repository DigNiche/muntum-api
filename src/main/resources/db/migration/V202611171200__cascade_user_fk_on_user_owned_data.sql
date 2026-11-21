-- 회원(users) 행이 삭제되면 그 회원의 스크랩·관심 키워드·프로그램 반응을 DB가 함께 지운다.
-- - 탈퇴 코드에서 세 테이블을 따로 지우지 않는다. 자식 삭제가 users 삭제와 한 번에 일어나
--   탈퇴 도중 같은 회원의 스크랩·반응 요청이 끼어들어도 탈퇴가 FK 오류(1451)로 실패하지 않는다.
-- - 세 테이블에는 저장된 카운터, Redis, S3가 연결되어 있지 않다(스크랩 수·반응 수는 조회 때 COUNT로 계산).
-- - 자동 생성된 FK 이름은 읽을 수 있는 이름으로 바꾼다. FK가 쓰는 user_id 인덱스는 그대로 둔다.
-- - FOREIGN_KEY_CHECKS는 끄지 않는다(주인 없는 행이 있으면 실패해야 한다).
-- - MySQL은 FK 검사를 켠 상태에서 같은 ALTER 안의 DROP/ADD FOREIGN KEY를 지원하지 않아 두 문장으로 나눈다.

-- 1. scraps
ALTER TABLE `scraps` DROP FOREIGN KEY `FKqd3nh3tj8ru0ubnk54qckuh42`;
ALTER TABLE `scraps`
    ADD CONSTRAINT `fk_scraps_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 2. user_keywords
ALTER TABLE `user_keywords` DROP FOREIGN KEY `FKau7mn5y9jdm7koto4fi1m26kw`;
ALTER TABLE `user_keywords`
    ADD CONSTRAINT `fk_user_keywords_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- 3. program_reactions
ALTER TABLE `program_reactions` DROP FOREIGN KEY `fk_program_reaction_user`;
ALTER TABLE `program_reactions`
    ADD CONSTRAINT `fk_program_reaction_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
