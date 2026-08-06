# LAN-261 TTS 음성 추가 설계

## 목표

`tts_voice` 테이블에 OpenRouter의 Deepgram Aura 2 남성 음성 레코드 한 건을 추가한다.

## 데이터

- `provider`: `OPENROUTER`
- `model`: `deepgram/aura-2`
- `provider_voice_id`: `aura-2-orpheus-en`
- `gender`: `MALE`
- `accent_locale`: `EN_US`
- `description`: `굵은 남성 음성`
- `status`: `ACTIVE`
- `created_at`, `updated_at`: `CURRENT_TIMESTAMP`

## 구현

새 Flyway 마이그레이션 `V38__add_deepgram_aura_2_tts_voice.sql`에서 위 레코드를 삽입한다. 기존 `tts_voice`의 `(provider, model, provider_voice_id)` 유니크 제약을 사용하며, 별도 엔티티나 API 변경은 하지 않는다.

## 검증

마이그레이션 통합 테스트에서 지정된 provider, model, provider voice ID, 성별, 로케일, 설명, 활성 상태가 저장됐는지 확인한다. 이후 `./gradlew check --rerun-tasks --no-daemon`으로 전체 검사를 실행한다.
