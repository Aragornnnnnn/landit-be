-- 사용자 프로필에 온보딩에서 선택한 영어 억양을 저장한다.

ALTER TABLE user_profile
    ADD COLUMN accent_locale VARCHAR(35) NOT NULL DEFAULT 'EN_US';
