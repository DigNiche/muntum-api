-- 관심 키워드(user_keywords)의 소프트 삭제 칸(deleted_at·deleted_by)을 없앤다.
--
-- 이유
--   관심 키워드는 바꿀 때 행을 바로 지우고, 회원이 탈퇴하면 DB가 함께 지운다(ON DELETE CASCADE).
--   두 칸을 채우는 코드는 없고, 조회의 deleted_at IS NULL 조건은 늘 참이라 함께 뺐다.
--
-- 기존 데이터
--   운영에 deleted_at·deleted_by 값이 있는 행은 없다(0건).

ALTER TABLE `user_keywords`
    DROP COLUMN `deleted_at`,
    DROP COLUMN `deleted_by`;
