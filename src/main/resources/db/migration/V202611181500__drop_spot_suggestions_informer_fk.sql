-- 제보자(spot_suggestions.informer)의 users FK를 없애고, FK 없는 UUID 칸으로 둔다.
--
-- 이유
--   informer는 "이 제보의 주인(제보로 생기는 혜택을 받을 사람)"이라는 서비스상 칸이라 created_by(행을 저장한 사람)와 따로 남긴다.
--   제보는 제보자가 탈퇴해도 남는 데이터다. 제보자가 탈퇴하면 이 칸을 탈퇴자 UUID(8888…/7777…/6666…)로 바꾸는데,
--   그 값은 users에 없어서 FK가 있으면 넣을 수 없다. 이제까지는 FK 때문에 NULL로 비워 왔다.
--
-- 인덱스
--   idx_suggestions_user_id(informer)는 남긴다. 내 제보 목록이 제보자로 검색한다.
--
-- 기존 데이터
--   이미 탈퇴해서 informer가 NULL이 된 행은 같은 행의 created_by로 채운다.
--   created_by는 탈퇴 때 역할별 탈퇴자 UUID로 바뀌어 있고, 제보는 본인이 등록하므로 두 칸은 같은 사람이다.
--   탈퇴자 UUID인 행만 채워, 다른 이유로 비어 있는 행은 건드리지 않는다.
--
-- 되돌리기
--   탈퇴자 UUID가 들어간 뒤에는 FK를 다시 걸 수 없다(users에 없는 값). 다시 걸려면 그 값들을 먼저 정리해야 한다.

ALTER TABLE `spot_suggestions` DROP FOREIGN KEY `FKg9lq7ip4ppw8sket8euwlgp2y`;

UPDATE `spot_suggestions`
SET `informer` = `created_by`
WHERE `informer` IS NULL
  AND (BIN_TO_UUID(`created_by`) LIKE '88888888-8888-8888-8888-%'
    OR BIN_TO_UUID(`created_by`) LIKE '77777777-7777-7777-7777-%'
    OR BIN_TO_UUID(`created_by`) LIKE '66666666-6666-6666-6666-%');
