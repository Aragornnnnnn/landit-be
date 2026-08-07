-- 프리톡 세션별 표현 학습 완료 시각을 저장한다.
ALTER TABLE free_talk_session_expression
    ADD COLUMN completed_at TIMESTAMP(6);
