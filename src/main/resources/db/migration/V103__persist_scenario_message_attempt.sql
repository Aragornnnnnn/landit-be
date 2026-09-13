-- 같은 발화의 재전송과 프로세스 종료 뒤 다음 질문 생성을 안전하게 재개한다.
ALTER TABLE session_history_message ADD COLUMN scenario_attempt_token VARCHAR(36);
ALTER TABLE session_history_message ADD COLUMN scenario_lease_until TIMESTAMP;
ALTER TABLE session_history_message ADD COLUMN scenario_response_payload TEXT;
