-- 표현 학습을 재완료(복습)한 마지막 시각을 기록하는 컬럼을 추가한다.
-- completed_at은 최초 완료 시각으로 유지하고, last_completed_at은 완료할 때마다 갱신한다.
ALTER TABLE user_writing_expression_completion ADD COLUMN last_completed_at TIMESTAMP(6);

-- 기존 완료 기록은 최초 완료 시각을 마지막 완료 시각으로 그대로 백필한다.
UPDATE user_writing_expression_completion
SET last_completed_at = completed_at
WHERE last_completed_at IS NULL;

ALTER TABLE user_writing_expression_completion ALTER COLUMN last_completed_at SET NOT NULL;
