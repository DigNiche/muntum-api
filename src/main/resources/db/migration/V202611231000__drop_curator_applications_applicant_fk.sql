-- 큐레이터 지원서의 지원자(curator_applications.applicant_id) users FK를 없애고, FK 없는 UUID 칸으로 둔다.
--
-- 이유
--   승인·반려로 검토가 확정된 지원서는 지원자가 탈퇴해도 남긴다(지원자가 문틈에 제공하기로 동의한 정보, AI 심사 학습 자료).
--   지원자가 탈퇴하면 이 칸을 탈퇴자 UUID(7777…/6666…)로 바꾸는데, 그 값은 users에 없어서 FK가 있으면 넣을 수 없다.
--   이제까지는 FK 때문에 탈퇴할 때 지원서를 모두 지워 왔다.
--
-- 인덱스
--   idx_curator_applications_applicant_id(applicant_id)는 남긴다. 내 지원 내역 조회가 지원자로 검색한다.
--
-- 기존 데이터
--   운영에 사라진 회원을 가리키는 값이 없어(탈퇴 때 지원서를 지워 왔음) 고칠 데이터는 없다.
--
-- 되돌리기
--   탈퇴자 UUID가 들어간 뒤에는 FK를 다시 걸 수 없다(users에 없는 값). 다시 걸려면 그 값들을 먼저 정리해야 한다.

ALTER TABLE `curator_applications` DROP FOREIGN KEY `fk_curator_applications_applicant`;
