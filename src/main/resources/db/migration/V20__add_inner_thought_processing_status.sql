ALTER TABLE session_history_message
    ADD COLUMN inner_thought_processing_status VARCHAR(20);

UPDATE session_history_message
SET inner_thought_processing_status = CASE
    WHEN inner_thought IS NOT NULL AND inner_thought_type IS NOT NULL THEN 'COMPLETED'
    ELSE 'FAILED'
END
WHERE role = 'USER';

ALTER TABLE session_history_message
    ADD CONSTRAINT chk_session_message_inner_thought_status
        CHECK (
            inner_thought_processing_status IS NULL
            OR inner_thought_processing_status IN ('PREPARING', 'COMPLETED', 'FAILED')
        );
