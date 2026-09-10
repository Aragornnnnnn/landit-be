-- 세션 수준 평가의 비동기 처리 상태와 예약 시각을 저장한다.
ALTER TABLE learning_session
    ADD COLUMN level_assessment_processing_status VARCHAR(20);

ALTER TABLE learning_session
    ADD COLUMN level_assessment_requested_at TIMESTAMP(6);

ALTER TABLE learning_session
    ADD CONSTRAINT chk_learning_session_level_assessment_processing_status
    CHECK (level_assessment_processing_status IN ('PREPARING', 'COMPLETED', 'FAILED')) NOT VALID;
