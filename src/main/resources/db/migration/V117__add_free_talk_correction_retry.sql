-- 턴 교정이 서버 재시작이나 AI의 일시적 실패로 끝나지 못하는 일을 막으려고, 교정 행 하나를 재시도 작업 한 건으로 다룬다.
-- 별도 작업 테이블을 두지 않고 교정 행에 시도 횟수와 임대(lease) 정보를 둔다.
ALTER TABLE free_talk_message_feedback ADD COLUMN attempts INTEGER DEFAULT 0 NOT NULL;
-- 이 시각까지는 누군가 처리 중이거나 다음 재시도를 기다리는 중이다. 애플리케이션 Clock(서울 시간) 기준으로만 쓰고 비교한다.
-- 이 컬럼이 생기기 전에 준비 상태로 남은 행은 NULL이고, NULL은 만료된 것으로 본다.
ALTER TABLE free_talk_message_feedback ADD COLUMN lease_until TIMESTAMP(6);
-- 복구 워커가 선점한 시도의 식별자. 늦게 끝난 옛 시도가 새 시도의 결과를 덮어쓰지 못하게 한다.
ALTER TABLE free_talk_message_feedback ADD COLUMN attempt_token VARCHAR(36);

ALTER TABLE free_talk_message_feedback
    ADD CONSTRAINT chk_free_talk_message_feedback_attempts CHECK (attempts >= 0);

-- 복구 워커는 준비 상태이면서 임대가 끝난 행만 주기적으로 찾는다.
CREATE INDEX idx_free_talk_message_feedback_recovery
    ON free_talk_message_feedback (processing_status, lease_until);
