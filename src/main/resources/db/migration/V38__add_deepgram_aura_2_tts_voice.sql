-- Deepgram Aura 2의 남성 TTS 음성을 추가한다.
INSERT INTO tts_voice (
    provider,
    model,
    provider_voice_id,
    gender,
    description,
    accent_locale,
    status,
    created_at,
    updated_at
)
VALUES (
    'OPENROUTER',
    'deepgram/aura-2',
    'aura-2-orpheus-en',
    'MALE',
    '굵은 남성 음성',
    'EN_US',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
