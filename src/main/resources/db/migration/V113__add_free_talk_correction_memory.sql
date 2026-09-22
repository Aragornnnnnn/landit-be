-- 장기기억을 근거로 한 턴 교정이 어떤 기억을 썼는지와 화면 태그("9/13 스몰톡에서 말한 헬스장")의 재료를 저장한다.
-- 지난 기록은 조회할 때마다 같아야 하므로 태그의 재료(날짜·라벨)를 교정과 함께 저장하고, 조회할 때 기억 테이블을 다시 읽지 않는다.
-- 기억이 나중에 덮어쓰이거나 지워져도 교정은 남아야 하므로 외래 키를 걸지 않는다.
ALTER TABLE free_talk_message_feedback
    ADD COLUMN memory_id BIGINT;

-- 그 기억을 말한 날짜. 교정을 저장하는 시점의 값을 그대로 남긴다.
ALTER TABLE free_talk_message_feedback
    ADD COLUMN memory_observed_on DATE;

-- AI가 돌려준 짧은 명사구. AI가 라벨을 주지 못했으면 NULL로 두고 조회할 때 기본 문구를 쓴다.
ALTER TABLE free_talk_message_feedback
    ADD COLUMN memory_label VARCHAR(40);

-- 근거 기억과 그 날짜는 함께 있거나 함께 없다. 라벨은 근거 기억이 있을 때만, 근거 기억은 교정 문장이 있을 때만 가질 수 있다.
ALTER TABLE free_talk_message_feedback
    ADD CONSTRAINT chk_free_talk_message_feedback_memory
        CHECK (
            (
                (memory_id IS NULL AND memory_observed_on IS NULL)
                OR (memory_id IS NOT NULL AND memory_observed_on IS NOT NULL)
            )
            AND (memory_label IS NULL OR memory_id IS NOT NULL)
            AND (memory_id IS NULL OR better_sentence IS NOT NULL)
        );
