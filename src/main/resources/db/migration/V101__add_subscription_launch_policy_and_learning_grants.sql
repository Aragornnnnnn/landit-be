-- 시작한 학습의 완료 권한과 사용자별 첫 무료 예약을 저장한다.
CREATE TABLE learning_access_grant (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    kind VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    policy_version BIGINT NOT NULL,
    basis VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);
CREATE INDEX idx_learning_access_grant_target ON learning_access_grant(user_id, kind, target_id, started_at);
CREATE TABLE free_scenario_reservation (
    user_id BIGINT PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE,
    session_id BIGINT NOT NULL UNIQUE REFERENCES learning_session(id),
    scenario_id BIGINT NOT NULL,
    reserved_at TIMESTAMP NOT NULL
);
