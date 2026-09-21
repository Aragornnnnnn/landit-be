-- 문의 없이 지정한 사용자에게 직접 편지를 보낼 수 있도록 저장 제약을 확장한다.
ALTER TABLE mailbox_letter DROP CONSTRAINT chk_mailbox_letter_type;
ALTER TABLE mailbox_letter ADD CONSTRAINT chk_mailbox_letter_type
    CHECK (letter_type IN ('NOTICE', 'UPDATE', 'REPLY', 'DIRECT'));

ALTER TABLE mailbox_letter DROP CONSTRAINT chk_mailbox_letter_payload;
ALTER TABLE mailbox_letter ADD CONSTRAINT chk_mailbox_letter_payload
    CHECK (
        (letter_type IN ('NOTICE', 'UPDATE')
            AND content_blocks IS NOT NULL AND body_text IS NULL)
        OR (letter_type IN ('REPLY', 'DIRECT')
            AND content_blocks IS NULL AND body_text IS NOT NULL)
    );

ALTER TABLE mailbox_letter_recipient ALTER COLUMN representative_feedback_id DROP NOT NULL;
