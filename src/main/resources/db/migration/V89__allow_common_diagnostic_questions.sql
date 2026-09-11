-- 기존 수준별 질문과 세션을 보존하면서 공통 진단 질문 그룹을 허용한다.
ALTER TABLE scenario_question DROP CONSTRAINT chk_scenario_question_level_group;
ALTER TABLE scenario_question ADD CONSTRAINT chk_scenario_question_level_group
    CHECK (question_level_group IN ('LEVEL_1', 'LEVEL_2_TO_3', 'LEVEL_4_TO_5', 'DIAGNOSTIC'));

ALTER TABLE scenario_session DROP CONSTRAINT chk_scenario_session_level_group;
ALTER TABLE scenario_session ADD CONSTRAINT chk_scenario_session_level_group
    CHECK (question_level_group IN ('LEVEL_1', 'LEVEL_2_TO_3', 'LEVEL_4_TO_5', 'DIAGNOSTIC'));
