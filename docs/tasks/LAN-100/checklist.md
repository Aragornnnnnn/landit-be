# LAN-100 AI 튜터와 시나리오 TTS 음성 분리 체크리스트

- [x] V14 마이그레이션 스키마 테스트를 작성하고 실패를 확인한다.
- [x] `tts_voice` 테이블과 초기 음성 2건을 추가한다.
- [x] 기본 미국 영어 튜터와 한국어 variant를 추가한다.
- [x] 기존 사용자의 `ai_tutor_id`를 backfill한다.
- [x] `ai_tutor`와 `scenario`의 기존 음성 컬럼을 제거한다.
- [x] `scenario_language_variant.tts_voice_id` FK를 추가한다.
- [x] 신규 회원 기본 튜터 설정 테스트를 작성하고 실패를 확인한다.
- [x] 기본 튜터 조회와 신규 프로필 설정을 구현한다.
- [x] 시나리오 목록 API의 `ttsVoice` 응답 테스트를 작성하고 실패를 확인한다.
- [x] 시나리오 목록 조회와 응답을 `ttsVoice` 객체로 변경한다.
- [x] 세션 시작 API의 `ttsVoice` 응답 테스트를 작성하고 실패를 확인한다.
- [x] 세션 시작 조회와 응답을 `ttsVoice` 객체로 변경한다.
- [x] 비활성 또는 미설정 TTS 음성이 `null`인지 검증한다.
- [x] Swagger/OpenAPI 응답 설명을 갱신한다.
- [x] `./gradlew test`를 실행한다.
- [x] 변경을 논리 단위 커밋으로 정리한다.
- [ ] PostgreSQL에서 V14 Flyway 마이그레이션을 실행한다.
- [ ] 운영 DB의 기존 `scenario.tts_voice_set_id` 값과 이전 결과를 확인한다.
