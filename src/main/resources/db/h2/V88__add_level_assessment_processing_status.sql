-- H2에서 세션 수준 평가의 상태와 예약 시각을 저장하고 허용 상태를 검증한다.
ALTER TABLE learning_session
    ADD COLUMN level_assessment_processing_status VARCHAR(20);

ALTER TABLE learning_session
    ADD COLUMN level_assessment_requested_at TIMESTAMP(6);

ALTER TABLE learning_session
    ADD CONSTRAINT chk_learning_session_level_assessment_processing_status
    CHECK (level_assessment_processing_status IN ('PREPARING', 'COMPLETED', 'FAILED'));
