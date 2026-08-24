-- 기존 학습 수준을 1부터 5까지의 정수 척도로 변환한다.
UPDATE user_profile
SET learning_level = CASE learning_level
    WHEN 'BEGINNER' THEN '1'
    WHEN 'INTERMEDIATE' THEN '3'
    WHEN 'ADVANCED' THEN '5'
    ELSE learning_level
END
WHERE learning_level IS NOT NULL;

ALTER TABLE user_profile
    ALTER COLUMN learning_level TYPE INTEGER;

ALTER TABLE user_profile
    ADD CONSTRAINT chk_user_profile_learning_level
        CHECK (learning_level IS NULL OR learning_level BETWEEN 1 AND 5);
