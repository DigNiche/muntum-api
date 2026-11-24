-- 회원(users)의 소프트 삭제 흔적을 없앤다: deleted_at·deleted_by 칸, status의 'DELETED' 값.
--
-- 이유
--   탈퇴는 회원 행을 바로 지우는 하드 삭제로 바뀌었다. 회원 행을 남겨 두고 DELETED로 표시하던 방식은 쓰지 않는다.
--   그래서 deleted_at·deleted_by는 값이 채워지지 않고, status가 DELETED인 회원도 생기지 않는다.
--
-- 기존 데이터
--   운영에 deleted_at·deleted_by 값이 있는 회원, status가 DELETED인 회원은 없다(0건).
--   status에 DELETED 값이 남아 있으면 아래 MODIFY가 실패하므로, 데이터가 바뀌는 일 없이 멈춘다.

ALTER TABLE `users`
    DROP COLUMN `deleted_at`,
    DROP COLUMN `deleted_by`,
    MODIFY `status` enum('ACTIVE','INACTIVE','PENDING','SUSPENDED') COLLATE utf8mb4_general_ci NOT NULL;
