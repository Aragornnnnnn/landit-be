-- 앞선 제약 추가 트랜잭션의 강한 잠금을 해제한 뒤 기존 행을 검증한다.
ALTER TABLE user_profile VALIDATE CONSTRAINT chk_user_profile_discount_offer;
