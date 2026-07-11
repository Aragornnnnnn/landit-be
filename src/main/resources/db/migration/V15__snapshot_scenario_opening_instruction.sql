-- USER First 평가에 사용할 시작 안내를 세션 생성 시점 값으로 보존한다.
ALTER TABLE scenario_session
    ADD COLUMN user_opening_instruction_snapshot TEXT;

UPDATE scenario_session
SET user_opening_instruction_snapshot = (
    SELECT variant.user_opening_instruction
    FROM scenario_language_variant variant
    JOIN scenario ON scenario.id = variant.scenario_id
    WHERE variant.id = scenario_session.scenario_language_variant_id
      AND scenario.first_speaker = 'USER'
)
WHERE EXISTS (
    SELECT 1
    FROM scenario_language_variant variant
    JOIN scenario ON scenario.id = variant.scenario_id
    WHERE variant.id = scenario_session.scenario_language_variant_id
      AND scenario.first_speaker = 'USER'
      AND variant.user_opening_instruction IS NOT NULL
);
