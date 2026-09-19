-- 프리톡 사용자 발화마다 AI가 만든 턴 교정과 처리 상태를 저장한다.
ALTER TABLE session_history_message
    ADD COLUMN correction_original TEXT;

ALTER TABLE session_history_message
    ADD COLUMN correction_better TEXT;

ALTER TABLE session_history_message
    ADD COLUMN correction_reason TEXT;

ALTER TABLE session_history_message
    ADD COLUMN mistake_pattern VARCHAR(40);

ALTER TABLE session_history_message
    ADD COLUMN reacted_to_partner BOOLEAN;

ALTER TABLE session_history_message
    ADD COLUMN correction_processing_status VARCHAR(20);

-- 교정을 만든 적 없는 기존 프리톡 사용자 발화는 "교정 없음"과 구분되도록 실패로 남긴다.
UPDATE session_history_message
SET correction_processing_status = 'FAILED'
WHERE role = 'USER'
  AND session_history_id IN (
      SELECT id FROM session_history WHERE session_type = 'FREE_TALK'
  );

ALTER TABLE session_history_message
    ADD CONSTRAINT chk_session_message_correction_status
        CHECK (
            correction_processing_status IS NULL
            OR correction_processing_status IN ('PREPARING', 'COMPLETED', 'FAILED')
        );

-- 교정문·원문·이유·실수 패턴은 함께 있거나 함께 없어야 한다.
ALTER TABLE session_history_message
    ADD CONSTRAINT chk_session_message_correction_fields
        CHECK (
            (
                correction_original IS NULL
                AND correction_better IS NULL
                AND correction_reason IS NULL
                AND mistake_pattern IS NULL
            )
            OR (
                correction_original IS NOT NULL
                AND correction_better IS NOT NULL
                AND correction_reason IS NOT NULL
                AND mistake_pattern IS NOT NULL
                AND correction_processing_status = 'COMPLETED'
            )
        );
