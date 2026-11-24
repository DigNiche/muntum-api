-- 회원(users)의 수정자 칸(updated_by)에 인덱스를 둔다.
--
-- 이유
--   관리자가 다른 회원 행을 바꾸면(예: 큐레이터 지원서 승인 때 지원자 역할 변경) 그 회원 행의 updated_by에
--   관리자 UUID가 자동으로 들어간다(BaseEntity @LastModifiedBy). 관리자가 탈퇴하면 이 값을 탈퇴자 UUID로 바꿔야 한다.
--   탈퇴 트랜잭션 안에서 UPDATE users ... WHERE updated_by = ? 를 실행하는데, 인덱스가 없으면 users 전체를 훑으며
--   모든 행을 잠가 탈퇴가 끝날 때까지 모든 회원의 요청(좋아요·관심 키워드·탈퇴)이 멈춘다.
--   인덱스가 있으면 해당하는 행만 잠근다.
--
-- 기존 데이터
--   운영에 사라진 회원을 가리키는 users.created_by·updated_by는 없어(0건) 고칠 데이터는 없다.

CREATE INDEX `idx_users_updated_by` ON `users` (`updated_by`);
