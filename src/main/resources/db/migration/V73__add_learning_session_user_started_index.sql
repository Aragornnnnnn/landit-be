-- 사용자별 최신 프리톡 세션 조회를 위한 정렬 인덱스를 추가한다.
CREATE INDEX idx_learning_session_user_profile_started_at
    ON learning_session (user_profile_id, started_at DESC, id DESC);
