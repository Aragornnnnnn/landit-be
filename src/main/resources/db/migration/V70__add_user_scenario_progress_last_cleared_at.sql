-- 시나리오 재완료를 포함한 마지막 실제 완료 시각을 기록한다.

ALTER TABLE user_scenario_progress
    ADD COLUMN last_cleared_at TIMESTAMP(6);

UPDATE user_scenario_progress
SET last_cleared_at = first_cleared_at
WHERE last_cleared_at IS NULL
  AND first_cleared_at IS NOT NULL;

CREATE INDEX idx_user_scenario_progress_last_cleared_at
    ON user_scenario_progress (user_profile_id, last_cleared_at);
