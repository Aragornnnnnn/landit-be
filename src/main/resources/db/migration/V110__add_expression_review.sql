-- 푸시로 제공한 복습 문제와 멱등 제출 결과 및 학습 알림 예약을 저장한다.

CREATE TABLE expression_review (
    id UUID PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profile(id),
    scheduled_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    available_until TIMESTAMP(6) NOT NULL,
    started_at TIMESTAMP(6),
    expires_at TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    CONSTRAINT uk_expression_review_schedule UNIQUE(user_profile_id, scheduled_date),
    CONSTRAINT chk_expression_review_start CHECK
        ((started_at IS NULL AND expires_at IS NULL) OR
         (started_at IS NOT NULL AND expires_at IS NOT NULL AND expires_at > started_at)),
    CONSTRAINT chk_expression_review_completion CHECK
        (completed_at IS NULL OR (started_at IS NOT NULL AND completed_at >= started_at))
);
CREATE INDEX ix_expression_review_user_created ON expression_review(user_profile_id, created_at);

CREATE TABLE expression_review_question (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES expression_review(id),
    expression_id BIGINT NOT NULL REFERENCES writing_expression(id),
    target_expression_text TEXT NOT NULL,
    base_expression_meaning_text TEXT NOT NULL,
    quiz_json TEXT NOT NULL,
    display_order INTEGER NOT NULL,
    queue_order INTEGER NOT NULL,
    wrong_count INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMP(6),
    CONSTRAINT uk_expression_review_question UNIQUE(review_id, expression_id),
    CONSTRAINT uk_expression_review_order UNIQUE(review_id, display_order),
    CONSTRAINT chk_expression_review_question_count CHECK
        (display_order BETWEEN 0 AND 2 AND queue_order >= 0 AND wrong_count >= 0)
);
CREATE INDEX ix_expression_review_question_expression ON expression_review_question(expression_id, review_id);

CREATE TABLE expression_review_submission (
    review_id UUID NOT NULL REFERENCES expression_review(id),
    submission_id UUID NOT NULL,
    question_id UUID NOT NULL REFERENCES expression_review_question(id),
    answer_json TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY(review_id, submission_id)
);

CREATE TABLE learning_notification_slot (
    event_id VARCHAR(160) PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profile(id),
    notification_group VARCHAR(20) NOT NULL,
    reserved_at TIMESTAMP(6) NOT NULL,
    reserved_date DATE NOT NULL,
    CONSTRAINT uk_learning_notification_daily_group
        UNIQUE(user_profile_id, reserved_date, notification_group)
);
CREATE INDEX ix_learning_notification_slot_user_time ON learning_notification_slot(user_profile_id, reserved_at);
