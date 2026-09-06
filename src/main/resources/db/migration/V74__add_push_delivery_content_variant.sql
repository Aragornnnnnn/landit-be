-- 푸시 알림 문구 변형을 발송 이력에 저장한다.

ALTER TABLE push_delivery
    ADD COLUMN content_variant VARCHAR(40);
