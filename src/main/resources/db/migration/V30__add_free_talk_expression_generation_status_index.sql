-- 준비 중인 프리톡 표현 생성을 빠르게 조회한다.
CREATE INDEX idx_free_talk_session_expression_generation_status
    ON free_talk_session (expression_generation_status);
