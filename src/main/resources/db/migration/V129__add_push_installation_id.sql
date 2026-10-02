ALTER TABLE user_push_token
    ADD COLUMN installation_id UUID;

ALTER TABLE user_push_token
    ADD CONSTRAINT uk_user_push_token_installation UNIQUE (installation_id);
