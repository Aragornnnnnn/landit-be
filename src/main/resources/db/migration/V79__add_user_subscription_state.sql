-- RevenueCat 웹훅으로 갱신하는 사용자 구독 상태를 user_profile에 저장한다.

ALTER TABLE user_profile
    ADD COLUMN subscription_status VARCHAR(30) NOT NULL DEFAULT 'NONE';

ALTER TABLE user_profile
    ADD COLUMN subscription_expires_at TIMESTAMP(6);

ALTER TABLE user_profile
    ADD COLUMN subscription_event_at TIMESTAMP(6);
