-- 시나리오 시작 메시지 구성 요소를 첫 고정 질문으로 옮긴다.
ALTER TABLE scenario_question_language_variant
    ADD COLUMN inner_thought TEXT;

ALTER TABLE scenario_question_language_variant
    ADD COLUMN inner_thought_type VARCHAR(20);

ALTER TABLE scenario_question_language_variant
    ADD CONSTRAINT chk_scenario_question_lang_inner_thought_pair CHECK (
        (inner_thought IS NULL AND inner_thought_type IS NULL)
        OR (inner_thought IS NOT NULL AND inner_thought_type IS NOT NULL)
    );

ALTER TABLE scenario_language_variant
    DROP COLUMN ai_opening_message;

ALTER TABLE scenario_language_variant
    DROP COLUMN ai_opening_message_translation;

ALTER TABLE scenario_language_variant
    DROP COLUMN ai_opening_inner_thought;

ALTER TABLE scenario_language_variant
    DROP COLUMN ai_opening_inner_thought_type;
