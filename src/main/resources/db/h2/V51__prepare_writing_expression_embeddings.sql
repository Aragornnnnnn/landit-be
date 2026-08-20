DELETE FROM free_talk_session_expression
WHERE writing_expression_id IN (
    SELECT id FROM writing_expression WHERE owner_user_profile_id IS NOT NULL
);

DELETE FROM user_writing_expression_completion
WHERE writing_expression_id IN (
    SELECT id FROM writing_expression WHERE owner_user_profile_id IS NOT NULL
);

DELETE FROM writing_expression
WHERE owner_user_profile_id IS NOT NULL;

ALTER TABLE writing_expression
    DROP CONSTRAINT fk_writing_expression_owner_user_profile_id;

ALTER TABLE writing_expression
    DROP CONSTRAINT chk_writing_expression_source;

ALTER TABLE writing_expression
    DROP COLUMN owner_user_profile_id;

ALTER TABLE writing_expression
    ADD COLUMN embedding VARCHAR(32767);

ALTER TABLE writing_expression
    ADD CONSTRAINT chk_writing_expression_scenario_source
    CHECK (
        (expression_source = 'SCENARIO' AND scenario_id IS NOT NULL)
        OR (expression_source = 'FREE_TALK' AND scenario_id IS NULL)
    );
