-- 사용자 탈퇴 상태를 저장하고 탈퇴 시 소셜 연결 값을 비울 수 있게 변경한다.
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP(6);

ALTER TABLE users ALTER COLUMN provider DROP NOT NULL;
ALTER TABLE users ALTER COLUMN sub DROP NOT NULL;
ALTER TABLE users ALTER COLUMN nickname DROP NOT NULL;
