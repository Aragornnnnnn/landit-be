-- 앱 설치별 푸시 상태를 저장하도록 알림 스키마를 확장한다.

ALTER TABLE user_push_token
    ADD COLUMN installation_id UUID;

UPDATE user_push_token
SET installation_id = CAST(
    '00000000-0000-0000-0000-' || LPAD(CAST(id AS VARCHAR), 12, '0')
    AS UUID
);

ALTER TABLE user_push_token
    ALTER COLUMN installation_id SET NOT NULL;

ALTER TABLE user_push_token
    ADD COLUMN push_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_push_token
    ALTER COLUMN push_enabled DROP DEFAULT;

ALTER TABLE user_push_token
    ALTER COLUMN token DROP NOT NULL;

ALTER TABLE user_push_token
    ALTER COLUMN status DROP NOT NULL;

UPDATE user_push_token
SET status = 'INVALID'
WHERE status = 'REVOKED';

ALTER TABLE user_push_token
    ADD CONSTRAINT uk_user_push_token_installation_id UNIQUE (installation_id);

CREATE INDEX idx_user_push_token_user_profile_push_enabled_status
    ON user_push_token (user_profile_id, push_enabled, status);
