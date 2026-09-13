-- 체험 알림의 초기 채널을 활성화하되 관리자가 변경한 설정은 보존한다.
ALTER TABLE trial_reminder_settings ALTER COLUMN push_enabled SET DEFAULT TRUE;
ALTER TABLE trial_reminder_settings ALTER COLUMN email_enabled SET DEFAULT TRUE;

UPDATE trial_reminder_settings
SET push_enabled = TRUE, email_enabled = TRUE
WHERE id = 1
  AND NOT EXISTS (
    SELECT 1 FROM admin_audit_log
    WHERE action = 'TRIAL_REMINDER_SETTINGS_UPDATED'
      AND target_type = 'TRIAL_REMINDER'
      AND target_id = '1'
  );
