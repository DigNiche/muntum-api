-- 기존 동의 데이터 유지
-- 검증된 약관 동의 증빙 기록이 누락된 경우를 허용

ALTER TABLE user_terms_agreements
    MODIFY COLUMN terms_of_service_at DATETIME(6) NULL,
    MODIFY COLUMN privacy_policy_at DATETIME(6) NULL;

-- 현재 약관 동의와 관련된 메타데이터 컬럼 추가
-- 새로운 약관 문서나 동의 기록(행)이 추가 생성되지는 않음

ALTER TABLE user_terms_agreements
    ADD COLUMN agreement_versions JSON NULL,
    ADD COLUMN signup_consent_source VARCHAR(32) NULL;