-- 평가가 비활성이어도 완료 당시 표현 학습 수준을 보존한다. 과거 세션은 소급 보정하지 않는다.
ALTER TABLE scenario_session ADD COLUMN learning_level_at_completion INTEGER
    CHECK (learning_level_at_completion BETWEEN 1 AND 5);
