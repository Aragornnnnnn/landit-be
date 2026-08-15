-- 시나리오와 프리톡이 공유하는 캐릭터와 TTS 음성 매핑을 추가한다.
CREATE TABLE conversation_character (
    character_id VARCHAR(20) PRIMARY KEY,
    tts_voice_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_conversation_character_tts_voice
        FOREIGN KEY (tts_voice_id) REFERENCES tts_voice (id),
    CONSTRAINT chk_conversation_character_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO conversation_character (
    character_id, tts_voice_id, status, created_at, updated_at
)
SELECT 'chloe', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tts_voice
WHERE provider = 'OPENROUTER'
  AND model = 'microsoft/mai-voice-2'
  AND provider_voice_id = 'en-US-Harper:MAI-Voice-2';

INSERT INTO conversation_character (
    character_id, tts_voice_id, status, created_at, updated_at
)
SELECT 'marco', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tts_voice
WHERE provider = 'OPENROUTER'
  AND model = 'deepgram/aura-2'
  AND provider_voice_id = 'aura-2-hyperion-en';

INSERT INTO conversation_character (
    character_id, tts_voice_id, status, created_at, updated_at
)
SELECT 'teddy', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM tts_voice
WHERE provider = 'OPENROUTER'
  AND model = 'deepgram/aura-2'
  AND provider_voice_id = 'aura-2-draco-en';

CREATE TABLE migration_v55_scenario_character_validation (
    violation_count BIGINT NOT NULL,
    CONSTRAINT chk_v55_scenario_character_mapping
        CHECK (violation_count = 0)
);

INSERT INTO migration_v55_scenario_character_validation (violation_count)
SELECT COUNT(*)
FROM (
    SELECT variant.scenario_id
    FROM scenario_language_variant variant
    LEFT JOIN conversation_character character
      ON character.tts_voice_id = variant.tts_voice_id
    GROUP BY variant.scenario_id
    HAVING SUM(
        CASE
            WHEN variant.tts_voice_id IS NOT NULL AND character.character_id IS NULL THEN 1
            ELSE 0
        END
    ) > 0
       OR COUNT(DISTINCT character.character_id) > 1
) invalid_scenario;

DROP TABLE migration_v55_scenario_character_validation;

ALTER TABLE scenario
    ADD COLUMN character_id VARCHAR(20);

UPDATE scenario
SET character_id = (
    SELECT MIN(character.character_id)
    FROM scenario_language_variant variant
    JOIN conversation_character character
      ON character.tts_voice_id = variant.tts_voice_id
    WHERE variant.scenario_id = scenario.id
);

ALTER TABLE scenario
    ADD CONSTRAINT fk_scenario_character
        FOREIGN KEY (character_id) REFERENCES conversation_character (character_id);

ALTER TABLE scenario_language_variant
    DROP CONSTRAINT fk_scenario_lang_tts_voice_id;

ALTER TABLE scenario_language_variant
    DROP COLUMN tts_voice_id;

ALTER TABLE free_talk_session
    ADD COLUMN character_id VARCHAR(20);

UPDATE free_talk_session
SET character_id = 'chloe'
WHERE character_id IS NULL;

ALTER TABLE free_talk_session
    ALTER COLUMN character_id SET NOT NULL;

ALTER TABLE free_talk_session
    ADD CONSTRAINT fk_free_talk_session_character
        FOREIGN KEY (character_id) REFERENCES conversation_character (character_id);
