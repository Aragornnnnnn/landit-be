-- 공개 정책과 시작한 학습의 완료 권한, 사용자별 첫 무료 예약을 분리한다.
CREATE TABLE subscription_launch_policy (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    version BIGINT NOT NULL,
    mode VARCHAR(10) NOT NULL CHECK (mode IN ('OFF', 'REVIEW', 'ALL')),
    effective_at TIMESTAMP,
    new_starts_paused BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (mode = 'OFF' OR effective_at IS NOT NULL)
);
CREATE TABLE subscription_review_user (
    policy_id BIGINT NOT NULL REFERENCES subscription_launch_policy(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    PRIMARY KEY (policy_id, user_id)
);
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
