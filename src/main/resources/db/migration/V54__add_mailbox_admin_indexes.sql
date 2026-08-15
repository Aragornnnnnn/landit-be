-- 편지함 어드민 목록의 상태·유형·작성 시각 필터를 지원한다.

CREATE INDEX idx_mailbox_letter_admin_filter
    ON mailbox_letter (publication_status, letter_type, created_at, id);

CREATE INDEX idx_mailbox_feedback_admin_filter
    ON mailbox_feedback (processing_status, feedback_type, created_at, id);
