-- 프리톡 계정별 일일 및 분당 생성 요청 수를 영속화한다.
ALTER TABLE free_talk_daily_speaking_usage
    ADD COLUMN request_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE free_talk_daily_speaking_usage
    ADD COLUMN request_minute TIMESTAMP;
ALTER TABLE free_talk_daily_speaking_usage
    ADD COLUMN minute_request_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE free_talk_daily_speaking_usage
    ADD CONSTRAINT chk_free_talk_request_count CHECK (request_count >= 0);
ALTER TABLE free_talk_daily_speaking_usage
    ADD CONSTRAINT chk_free_talk_minute_request_count CHECK (minute_request_count >= 0);
