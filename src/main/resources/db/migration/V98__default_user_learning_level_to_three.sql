-- 미설정 학습 수준만 3으로 보정하고 신규 행의 기본 수준을 지정한다. 평가 확정 이력은 만들지 않는다.
UPDATE user_profile SET learning_level = 3 WHERE learning_level IS NULL;
ALTER TABLE user_profile ALTER COLUMN learning_level SET DEFAULT 3;
