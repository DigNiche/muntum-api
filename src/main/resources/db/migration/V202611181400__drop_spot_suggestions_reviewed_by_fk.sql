-- 제보 검토자(spot_suggestions.reviewed_by)의 users FK를 없애고, 다른 감사 칸처럼 FK 없는 UUID 칸으로 둔다.
--
-- 이유
--   제보는 검토한 관리자가 탈퇴해도 남는 데이터다. 관리자가 탈퇴하면 이 칸을 탈퇴자 UUID(8888…)로 바꾸는데,
--   그 값은 users에 없어서 FK가 있으면 넣을 수 없다. 이제까지는 FK 때문에 NULL로 비워 왔다.
--   created_by·updated_by·deleted_by처럼 FK 없이 두고, 화면 표시는 공통 조회(UserDisplayResolver)가 맡는다.
--
-- 인덱스
--   FKooh5fxxsri4yxmvu9x8wr31md 인덱스는 FK를 만들 때 MySQL이 자동으로 만든 것이다. FK와 함께 지운다.
--   검토자로 찾는 읽기 쿼리가 없고, 다른 감사 칸(created_by·updated_by·deleted_by)에도 인덱스가 없다.
--   검토자로 검색하는 화면이 생기면 그때 만든다.
--
-- 되돌리기
--   탈퇴자 UUID가 들어간 뒤에는 FK를 다시 걸 수 없다(users에 없는 값). 다시 걸려면 그 값들을 먼저 정리해야 한다.

ALTER TABLE `spot_suggestions` DROP FOREIGN KEY `FKooh5fxxsri4yxmvu9x8wr31md`;
ALTER TABLE `spot_suggestions` DROP INDEX `FKooh5fxxsri4yxmvu9x8wr31md`;
