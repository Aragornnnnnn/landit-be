-- 체험 알림과 관리자 이메일 테스트의 예약 의도 및 채널별 발송 결과를 저장한다.
CREATE TABLE notification_job (
    id UUID PRIMARY KEY,
    kind VARCHAR(30) NOT NULL,
    user_profile_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    product_id VARCHAR(255),
    store VARCHAR(30),
    environment VARCHAR(20),
    expires_at TIMESTAMP WITH TIME ZONE,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recipient VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reservation_state VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claim_token UUID,
    provider_message_id VARCHAR(255),
    result_code VARCHAR(60),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_notification_job_reservation ON notification_job(reservation_state, next_attempt_at);
CREATE TABLE trial_reminder_settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO trial_reminder_settings (id, push_enabled, email_enabled) VALUES (1, FALSE, FALSE);
