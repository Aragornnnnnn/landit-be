-- 시나리오 질문과 진행 중 세션의 학습 레벨 그룹을 저장한다.
ALTER TABLE scenario_question ADD COLUMN question_level_group VARCHAR(30);

UPDATE scenario_question
SET question_level_group = 'LEVEL_4_TO_5';

-- 기존 시나리오 표현은 신규 레벨별 표현과 구분해 레벨 4~5 사용자에게 유지한다.
UPDATE writing_expression
SET difficulty_level = 4
WHERE expression_source = 'SCENARIO'
  AND difficulty_level < 4;

ALTER TABLE scenario_question ALTER COLUMN question_level_group SET NOT NULL;
ALTER TABLE scenario_question DROP CONSTRAINT uk_scenario_question_scenario_order;
ALTER TABLE scenario_question
    ADD CONSTRAINT uk_scenario_question_scenario_level_order
        UNIQUE (scenario_id, question_level_group, display_order);
ALTER TABLE scenario_question
    ADD CONSTRAINT chk_scenario_question_level_group
        CHECK (question_level_group IN ('LEVEL_1', 'LEVEL_2_TO_3', 'LEVEL_4_TO_5'));

ALTER TABLE scenario_session ADD COLUMN question_level_group VARCHAR(30);

UPDATE scenario_session
SET question_level_group = 'LEVEL_4_TO_5';

ALTER TABLE scenario_session ALTER COLUMN question_level_group SET NOT NULL;
ALTER TABLE scenario_session
    ADD CONSTRAINT chk_scenario_session_level_group
        CHECK (question_level_group IN ('LEVEL_1', 'LEVEL_2_TO_3', 'LEVEL_4_TO_5'));
