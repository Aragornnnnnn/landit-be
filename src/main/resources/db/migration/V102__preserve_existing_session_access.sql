-- 기존 코드가 시작한 진행 중 세션도 원래 시작 시각부터 24시간 동안 이어갈 수 있게 한다.
INSERT INTO learning_access_grant
(id, user_id, kind, target_id, policy_version, basis, started_at, expires_at)
SELECT 'legacy-' || id, user_profile_id, session_type, id, 0, 'BEFORE_LAUNCH',
       started_at, started_at + INTERVAL '24' HOUR
FROM learning_session WHERE status = 'IN_PROGRESS';
