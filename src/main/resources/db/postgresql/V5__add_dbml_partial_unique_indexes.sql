-- PostgreSQL에서 ACTIVE 상태 row에만 적용되는 partial unique index를 추가한다.
CREATE UNIQUE INDEX uk_oauth_identity_active_provider_user
    ON oauth_identity (provider, provider_user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uk_oauth_identity_active_user_provider
    ON oauth_identity (user_profile_id, provider)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uk_user_quest_active_user
    ON user_quest (user_profile_id)
    WHERE status = 'ACTIVE';
