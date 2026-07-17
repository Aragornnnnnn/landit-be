-- 사용자 발화의 메시지별 피드백 처리 상태를 저장한다.
ALTER TABLE session_history_message
    ADD COLUMN feedback_processing_status VARCHAR(20);

UPDATE session_history_message
SET feedback_processing_status = 'PREPARING'
WHERE role = 'USER';

ALTER TABLE session_history_message
    ADD CONSTRAINT chk_session_message_feedback_status
        CHECK (
            feedback_processing_status IS NULL
            OR feedback_processing_status IN ('PREPARING', 'COMPLETED', 'FAILED')
        );
