-- 외부 예약 등록 시도 횟수를 저장해 영구 실패의 무한 재시도를 제한한다.
ALTER TABLE notification_job
    ADD COLUMN reservation_attempts INTEGER NOT NULL DEFAULT 0;
