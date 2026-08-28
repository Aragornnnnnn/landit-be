-- 완료 프리톡의 장기기억 생성 작업 상태와 실행 시각을 저장한다.
ALTER TABLE free_talk_session
    ADD COLUMN memory_generation_status VARCHAR(20);
ALTER TABLE free_talk_session
    ADD COLUMN memory_generation_started_at TIMESTAMP(6);
ALTER TABLE free_talk_session
    ADD CONSTRAINT chk_free_talk_session_memory_generation_status
        CHECK (
            memory_generation_status IS NULL
            OR memory_generation_status IN ('PREPARING', 'READY', 'FAILED')
        );
ALTER TABLE free_talk_session
    ADD CONSTRAINT chk_free_talk_session_memory_generation_state
        CHECK (
            (memory_generation_status IS NULL AND memory_generation_started_at IS NULL)
            OR memory_generation_status = 'PREPARING'
            OR (
                memory_generation_status IN ('READY', 'FAILED')
                AND memory_generation_started_at IS NULL
            )
        );

CREATE INDEX idx_free_talk_session_memory_generation_status
    ON free_talk_session (memory_generation_status);
