# LAN-100 AI 튜터와 시나리오 TTS 음성 분리 컨텍스트 노트

## 2026-07-10

- 작업 브랜치는 최신 `origin/develop`에서 분기한 `feat/LAN-100`이다.
- `origin/develop`의 최신 공통 마이그레이션은 V11이지만 선행 PR에 V12와 V13이 있으므로 이번 이슈는 V14를 사용한다.
- V14를 운영 환경에 적용하기 전에 V12와 V13이 먼저 병합되고 배포되어야 한다.
- 시나리오 TTS 음성은 `scenario`가 아니라 `scenario_language_variant`가 nullable FK로 참조한다.
- 초기 음성은 Harper 남성 음성과 Ethan 여성 음성 2건이다.
- `(provider, model, provider_voice_id)` 조합에 유니크 제약을 둔다.
- 기존 `scenario.tts_voice_set_id`와 신규 Provider 음성 ID가 정확히 일치하는 경우에만 variant FK를 이전하고, 매핑할 수 없는 값은 null로 둔다.
- API에서는 내부 `tts_voice.id`를 노출하지 않고 `provider`, `model`, `providerVoiceId`, `gender`를 `ttsVoice` 객체로 반환한다.
- TTS 음성이 없거나 `INACTIVE`이면 `ttsVoice`는 null이다.
- `ttsVoice`는 첫 발화자와 관계없이 AI first와 USER first 응답에 동일하게 적용한다.
- 기본 AI 튜터는 `accent_locale=EN-US`, `target_locale=EN`, `status=ACTIVE` 조건으로 조회하며 Java 코드에 ID를 하드코딩하지 않는다.
- LAN-100에서 생성하는 locale 값은 대문자로 저장한다. 기본 AI 튜터 language variant의 `base_locale`은 서비스 기준 국가 코드 `KR`을 사용한다. `provider_voice_id`는 외부 Provider 식별자이므로 원문 `en-US-...` 형식을 유지한다.
- 기본 튜터 후보가 없거나 여러 개이면 신규 프로필을 저장하지 않고 서버 오류로 처리한다.
- V14는 기존 `scenario.tts_voice_set_id`가 신규 Provider 음성 ID와 정확히 일치할 때만 variant FK로 이전한다.
- 매핑되지 않는 기존 TTS 값은 nullable인 `scenario_language_variant.tts_voice_id`에 값을 설정하지 않는다.
- Google과 Apple 신규 회원 모두 기본 튜터가 설정되는 통합 테스트를 추가했다.
- 기본 튜터가 없거나 여러 개인 경우 `DEFAULT_AI_TUTOR_NOT_CONFIGURED` 500 응답을 검증했다.
- 시나리오 목록과 세션 시작 API에서 AI first와 USER first의 `ttsVoice` 객체를 검증했다.
- 비활성 또는 미설정 TTS 음성은 LEFT JOIN 결과를 비워 `ttsVoice = null`로 반환한다.
- `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests`가 통과했다.
- `./gradlew test` 전체 테스트가 통과했다.
- UseCase와 조회 조립 클래스의 메서드 분리가 많아 흐름을 따라가기 어려울 수 있으므로, 조건 검증, 조회, 생성, 응답 조립 책임이 드러나지 않는 메서드에 Javadoc을 보강했다.
- 로컬 환경에 Docker와 `psql`이 없어 PostgreSQL Flyway 실행은 수행하지 못했다.
- 운영 DB 접근 없이 작업했으므로 기존 `scenario.tts_voice_set_id` 값별 건수와 실제 이전 결과 확인은 남아 있다.
