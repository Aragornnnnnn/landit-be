-- Writing 표현을 시나리오와 프리톡 사용 영역으로 구분한다.
ALTER TABLE writing_expression DROP CONSTRAINT chk_writing_expression_source;

ALTER TABLE writing_expression
    ADD COLUMN expression_source VARCHAR(20) DEFAULT 'SCENARIO';

UPDATE writing_expression
SET expression_source = CASE
    WHEN scenario_id IS NULL THEN 'FREE_TALK'
    ELSE 'SCENARIO'
END;

ALTER TABLE writing_expression ALTER COLUMN expression_source SET NOT NULL;

ALTER TABLE writing_expression ADD CONSTRAINT chk_writing_expression_source
    CHECK (
        (expression_source = 'SCENARIO'
            AND scenario_id IS NOT NULL
            AND owner_user_profile_id IS NULL)
        OR (expression_source = 'FREE_TALK' AND scenario_id IS NULL)
    );
