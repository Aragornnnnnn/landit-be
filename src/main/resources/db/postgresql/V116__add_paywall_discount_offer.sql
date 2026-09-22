-- 계정별 최초 이탈 할인 만료 시각과 부여 당시 신규 혜택 여부를 저장한다.
ALTER TABLE user_profile ADD COLUMN discount_offer_expires_at TIMESTAMP(6);
ALTER TABLE user_profile ADD COLUMN discount_offer_new_user BOOLEAN;
ALTER TABLE user_profile ADD CONSTRAINT chk_user_profile_discount_offer
    CHECK ((discount_offer_expires_at IS NULL AND discount_offer_new_user IS NULL)
        OR (discount_offer_expires_at IS NOT NULL AND discount_offer_new_user IS NOT NULL)) NOT VALID;
