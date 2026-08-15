-- 프리톡 세션에 사용자가 선택한 캐릭터를 저장한다.
ALTER TABLE free_talk_session
    ADD COLUMN character_id VARCHAR(20);

UPDATE free_talk_session
SET character_id = 'chloe'
WHERE character_id IS NULL;

ALTER TABLE free_talk_session
    ALTER COLUMN character_id SET NOT NULL;

ALTER TABLE free_talk_session
    ADD CONSTRAINT chk_free_talk_session_character
        CHECK (character_id IN ('chloe', 'marco', 'teddy'));
