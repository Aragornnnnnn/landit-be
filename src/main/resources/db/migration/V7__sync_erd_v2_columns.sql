-- ERD v2 기준으로 시나리오와 Writing 컬럼 차이를 반영한다.
ALTER TABLE scenario ADD COLUMN total_question_count INTEGER;

UPDATE scenario
SET total_question_count = max_turns_to_goal
WHERE total_question_count IS NULL;

ALTER TABLE scenario ALTER COLUMN total_question_count SET NOT NULL;
ALTER TABLE scenario ADD CONSTRAINT chk_scenario_total_question_count CHECK (total_question_count > 0);
ALTER TABLE scenario DROP CONSTRAINT chk_scenario_turns;
ALTER TABLE scenario DROP COLUMN min_turns_to_goal;
ALTER TABLE scenario DROP COLUMN max_turns_to_goal;

ALTER TABLE writing_expression
    ADD COLUMN representative_sentence_translation_highlight_text VARCHAR(255);

UPDATE writing_expression
SET representative_sentence_translation_highlight_text =
        LEFT(representative_sentence_translation, 255)
WHERE representative_sentence_translation_highlight_text IS NULL;

ALTER TABLE writing_expression
    ALTER COLUMN representative_sentence_translation_highlight_text SET NOT NULL;

ALTER TABLE user_writing_expression_completion ADD COLUMN scenario_id BIGINT;

UPDATE user_writing_expression_completion
SET scenario_id = (
    SELECT writing_expression.scenario_id
    FROM writing_expression
    WHERE writing_expression.id = user_writing_expression_completion.writing_expression_id
)
WHERE scenario_id IS NULL;

ALTER TABLE user_writing_expression_completion ALTER COLUMN scenario_id SET NOT NULL;
ALTER TABLE user_writing_expression_completion
    ADD CONSTRAINT fk_user_writing_expression_completion_scenario_id
        FOREIGN KEY (scenario_id) REFERENCES scenario (id);
