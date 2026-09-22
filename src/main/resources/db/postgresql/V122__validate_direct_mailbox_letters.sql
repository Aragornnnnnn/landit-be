-- 앞선 제약 추가 트랜잭션의 강한 잠금을 해제한 뒤 기존 행을 검증한다.
ALTER TABLE mailbox_letter VALIDATE CONSTRAINT chk_mailbox_letter_type;
ALTER TABLE mailbox_letter VALIDATE CONSTRAINT chk_mailbox_letter_payload;
