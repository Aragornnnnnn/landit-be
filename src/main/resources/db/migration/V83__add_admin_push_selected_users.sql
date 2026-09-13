-- 기존 캠페인은 전체 발송으로 유지하고 선택 사용자 목록을 저장한다.
ALTER TABLE admin_push_campaign
    ADD COLUMN audience_type VARCHAR(20) NOT NULL DEFAULT 'ALL'
    CHECK (audience_type IN ('ALL', 'SELECTED'));

CREATE TABLE admin_push_campaign_user (
    campaign_id UUID NOT NULL REFERENCES admin_push_campaign(id),
    user_profile_id BIGINT NOT NULL REFERENCES user_profile(id),
    PRIMARY KEY (campaign_id, user_profile_id)
);
