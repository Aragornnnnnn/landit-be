-- SQL 대상 조건과 일회성 예약 상태를 저장한다. 설문 테이블은 외부 소유로 유지한다.
ALTER TABLE admin_push_campaign ADD COLUMN audience_sql TEXT;
ALTER TABLE admin_push_campaign ADD COLUMN scheduled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE admin_push_campaign_user ADD COLUMN excluded BOOLEAN NOT NULL DEFAULT FALSE;
