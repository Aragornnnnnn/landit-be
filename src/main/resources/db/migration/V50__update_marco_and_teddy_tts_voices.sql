-- Marco와 Teddy의 TTS 음성 모델, 설명, 억양을 변경한다.
UPDATE tts_voice
SET model = 'deepgram/aura-2',
    provider_voice_id = 'aura-2-hyperion-en',
    description = '호주 영어 남성 음성',
    accent_locale = 'EN_AU',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2;

UPDATE tts_voice
SET model = 'deepgram/aura-2',
    provider_voice_id = 'aura-2-draco-en',
    description = '영국 영어 굵은 남성 음성',
    accent_locale = 'EN_GB',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3;
