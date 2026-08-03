-- 시나리오와 프리톡의 표현 완료 이력을 분리한다.
ALTER TABLE user_writing_expression_completion
    ADD COLUMN learning_source VARCHAR(20) NOT NULL DEFAULT 'SCENARIO';

ALTER TABLE user_writing_expression_completion
    ADD CONSTRAINT chk_user_writing_expression_completion_source
    CHECK (learning_source IN ('SCENARIO', 'FREE_TALK'));

ALTER TABLE user_writing_expression_completion
    DROP CONSTRAINT uk_user_writing_expression_completion;

ALTER TABLE user_writing_expression_completion
    ADD CONSTRAINT uk_user_writing_expression_completion_source
    UNIQUE (user_profile_id, writing_expression_id, learning_source);
