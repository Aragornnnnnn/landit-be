-- 관찰하지 못한 수준값을 비워 두고 평가 당시 근거 충분성을 저장한다.
ALTER TABLE user_level_assessment ALTER COLUMN situation_performance_score DROP NOT NULL;
ALTER TABLE user_level_assessment ALTER COLUMN grammar_score DROP NOT NULL;
ALTER TABLE user_level_assessment ALTER COLUMN vocabulary_score DROP NOT NULL;
ALTER TABLE user_level_assessment ALTER COLUMN discourse_score DROP NOT NULL;
ALTER TABLE user_level_assessment ALTER COLUMN interaction_pragmatics_score DROP NOT NULL;
ALTER TABLE user_level_assessment ALTER COLUMN assessed_score DROP NOT NULL;
ALTER TABLE user_level_assessment ALTER COLUMN assessed_level DROP NOT NULL;
ALTER TABLE user_level_assessment ADD COLUMN sufficient_evidence BOOLEAN NOT NULL DEFAULT FALSE;

-- 이전 버전의 평가값은 재계산하지 않는다. 새 결과부터 충분성을 명시한다.
ALTER TABLE user_level_assessment ADD CONSTRAINT chk_user_level_assessment_score_pair
    CHECK ((assessed_score IS NULL AND assessed_level IS NULL)
        OR (assessed_score IS NOT NULL AND assessed_level IS NOT NULL));
