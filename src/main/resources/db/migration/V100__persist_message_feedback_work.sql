-- 메시지 평가 요청과 완성 결과를 BE에 보존해 AI 교체와 응답 유실을 복구한다.
CREATE TABLE message_feedback_work (
    message_id BIGINT PRIMARY KEY REFERENCES session_history_message(id) ON DELETE CASCADE,
    session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
    request_payload TEXT NOT NULL,
    result_payload TEXT,
    legacy_completed BOOLEAN NOT NULL DEFAULT FALSE,
    terminal_failed BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_token VARCHAR(36),
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL,
    lease_until TIMESTAMP
);
CREATE INDEX idx_message_feedback_work_recovery ON message_feedback_work(available_at, lease_until);
