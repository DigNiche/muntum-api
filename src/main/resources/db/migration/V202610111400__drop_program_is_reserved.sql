-- =========================================================
-- Drop programs.is_reserved
-- - 예약 필요 여부와 '예약없이' 필터는 reservation_type 하나로 관리
-- - 코드에서 reserved 제거와 같은 배포에 포함
--   (NOT NULL·기본값 없는 컬럼이라 컬럼만 남기면 프로그램 등록 INSERT가 실패함)
-- =========================================================

ALTER TABLE `programs`
DROP COLUMN `is_reserved`;