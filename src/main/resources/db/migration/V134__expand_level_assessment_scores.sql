-- 과거 평가값과 버전을 보존하면서 100점 평가를 저장할 정밀도를 확장한다.
ALTER TABLE user_level_assessment ALTER COLUMN situation_performance_score TYPE NUMERIC(5, 2);
ALTER TABLE user_level_assessment ALTER COLUMN grammar_score TYPE NUMERIC(5, 2);
ALTER TABLE user_level_assessment ALTER COLUMN vocabulary_score TYPE NUMERIC(5, 2);
ALTER TABLE user_level_assessment ALTER COLUMN discourse_score TYPE NUMERIC(5, 2);
ALTER TABLE user_level_assessment ALTER COLUMN interaction_pragmatics_score TYPE NUMERIC(5, 2);
ALTER TABLE user_level_assessment ALTER COLUMN assessed_score TYPE NUMERIC(5, 2);
