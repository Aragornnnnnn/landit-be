-- 시나리오 총평에 검증된 직전 교정 비교와 배운 표현 재사용 결과를 저장한다.
ALTER TABLE session_history_summary_feedback
    ADD COLUMN growth_feedback_payload JSONB;

ALTER TABLE session_history_summary_feedback
    ADD COLUMN expression_reuse_payload JSONB NOT NULL DEFAULT '{"pending": false, "items": []}'::jsonb;
