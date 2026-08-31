-- 활성 Expo Push Token 보유자의 푸시 권한 상태를 허용으로 보정한다.

UPDATE user_profile
SET push_permission_status = 'GRANTED',
    push_permission_updated_at = CURRENT_TIMESTAMP
WHERE push_permission_status = 'NOT_DETERMINED'
  AND user_profile.status = 'ACTIVE'
  AND EXISTS (
      SELECT 1
      FROM user_push_token token
      WHERE token.user_profile_id = user_profile.id
        AND token.status = 'ACTIVE'
  );
