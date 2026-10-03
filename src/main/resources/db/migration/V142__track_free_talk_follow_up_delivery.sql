-- 스몰톡 예고 질문의 선점과 실제 첫 AI 발화 저장을 구분해 기록한다.
ALTER TABLE free_talk_follow_up
    ADD COLUMN claimed_free_talk_session_id BIGINT;
ALTER TABLE free_talk_follow_up
    ADD COLUMN claim_expires_at TIMESTAMP(6);
ALTER TABLE free_talk_follow_up
    ADD COLUMN asked_at TIMESTAMP(6);
ALTER TABLE free_talk_follow_up
    ADD COLUMN asked_message_id BIGINT;

ALTER TABLE free_talk_follow_up
    ADD CONSTRAINT chk_free_talk_follow_up_claim
    CHECK ((claimed_free_talk_session_id IS NULL) = (claim_expires_at IS NULL));
ALTER TABLE free_talk_follow_up
    ADD CONSTRAINT chk_free_talk_follow_up_asked
    CHECK (asked_at IS NULL OR (asked_message_id IS NOT NULL AND claimed_free_talk_session_id IS NOT NULL));

CREATE INDEX idx_free_talk_follow_up_available
    ON free_talk_follow_up (user_profile_id, asked_at, created_at DESC);
