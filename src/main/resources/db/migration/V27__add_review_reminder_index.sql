-- 복습 리마인더 대상 조회를 위한 복합 인덱스를 추가한다.

CREATE INDEX idx_review_item_reminder_target
    ON review_item (review_date, status, user_profile_id);
