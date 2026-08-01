-- 기존 범용 Push Token을 Expo Push Token 전용 구조로 전환한다.

ALTER TABLE user_push_token
    RENAME COLUMN token TO expo_push_token;

UPDATE user_push_token
SET status = 'REVOKED'
WHERE status = 'ACTIVE';
