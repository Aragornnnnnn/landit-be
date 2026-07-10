# LAN-100 AI Tutor And Scenario TTS Voice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use test-driven-development and execute each task with a red-green-refactor cycle.

**Goal:** AI 튜터에서 음성 설정을 분리하고 언어별 시나리오 variant가 참조하는 TTS 음성을 API로 반환한다.

**Architecture:** Flyway V14가 스키마와 초기 데이터를 한 번에 전환한다. Auth 흐름은 기본 AI 튜터를 locale과 활성 상태로 조회해 신규 프로필에 설정한다. 시나리오 목록과 세션 시작 조회는 활성 TTS 음성을 LEFT JOIN하고 공유 응답 DTO로 변환한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL, H2, MockMvc, AssertJ.

## Global Constraints

- 이미 적용된 V4 마이그레이션은 수정하지 않는다.
- 신규 공통 마이그레이션은 `V14__separate_ai_tutor_and_scenario_tts_voice.sql`로 작성한다.
- 내부 `tts_voice.id`는 API에 노출하지 않는다.
- TTS 음성이 없거나 비활성이면 API의 `ttsVoice`는 null이다.
- API 필드는 기존 `ttsVoiceSetId`에서 `ttsVoice` 객체로 변경한다.
- 각 구현 전에 실패하는 테스트를 먼저 확인한다.

---

### Task 1: V14 스키마와 초기 데이터

**Files:**
- Create: `src/main/resources/db/migration/V14__separate_ai_tutor_and_scenario_tts_voice.sql`
- Modify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`

**Produces:** `tts_voice` 테이블, 초기 음성 2건, 기본 AI 튜터, `scenario_language_variant.tts_voice_id` FK, 기존 사용자 backfill.

- [ ] 스키마, 유니크 제약, FK, 초기 데이터와 제거 컬럼을 검증하는 실패 테스트를 추가한다.
- [ ] `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests`로 RED를 확인한다.
- [ ] V14 마이그레이션을 구현한다.
- [ ] 같은 테스트로 GREEN을 확인한다.
- [ ] `feat: AI 튜터와 시나리오 TTS 음성 스키마 분리`로 커밋한다.

### Task 2: 신규 회원 기본 AI 튜터 설정

**Files:**
- Create: `src/main/java/com/landit/landitbe/content/infrastructure/AiTutorRepository.java`
- Modify: `src/main/java/com/landit/landitbe/content/domain/AiTutor.java`
- Modify: `src/main/java/com/landit/landitbe/auth/domain/UserProfile.java`
- Modify: `src/main/java/com/landit/landitbe/auth/application/AuthService.java`
- Modify: `src/main/java/com/landit/landitbe/common/exception/ErrorCode.java`
- Modify: `src/test/java/com/landit/landitbe/auth/SocialAuthApiIntegrationTests.java`

**Produces:** 신규 프로필의 `aiTutorId` 설정과 기본 튜터 누락·중복 방어.

- [ ] Google과 Apple 신규 회원의 기본 튜터 설정 테스트를 추가한다.
- [ ] 테스트를 실행해 `ai_tutor_id` 미설정 실패를 확인한다.
- [ ] 기본 튜터 조회와 프로필 생성 로직을 구현한다.
- [ ] 관련 인증 테스트와 전체 인증 테스트를 통과시킨다.
- [ ] `feat: 신규 회원 기본 AI 튜터 설정`으로 커밋한다.

### Task 3: TTS 음성 도메인과 공통 API 응답

**Files:**
- Create: `src/main/java/com/landit/landitbe/content/domain/TtsVoice.java`
- Create: `src/main/java/com/landit/landitbe/content/domain/TtsVoiceProvider.java`
- Create: `src/main/java/com/landit/landitbe/content/domain/TtsVoiceGender.java`
- Create: `src/main/java/com/landit/landitbe/content/infrastructure/TtsVoiceRepository.java`
- Create: `src/main/java/com/landit/landitbe/content/api/dto/TtsVoiceResponse.java`
- Modify: `src/main/java/com/landit/landitbe/content/domain/AiTutor.java`
- Modify: `src/main/java/com/landit/landitbe/content/domain/Scenario.java`
- Modify: `src/main/java/com/landit/landitbe/content/domain/ScenarioLanguageVariant.java`

**Produces:** JPA 스키마 매핑과 공유 `TtsVoiceResponse` 계약.

- [ ] 엔티티와 enum을 V14 스키마에 맞춰 구현한다.
- [ ] 기존 음성 필드 매핑을 제거한다.
- [ ] 컴파일과 스키마 테스트를 통과시킨다.
- [ ] `feat: 시나리오 TTS 음성 도메인 추가`로 커밋한다.

### Task 4: 시나리오 목록 API 변경

**Files:**
- Modify: `src/main/java/com/landit/landitbe/content/infrastructure/ScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/content/infrastructure/ScenarioListRow.java`
- Modify: `src/main/java/com/landit/landitbe/content/application/ScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/content/api/dto/ScenarioListResponse.java`
- Modify: `src/test/java/com/landit/landitbe/content/ScenarioListApiIntegrationTests.java`

**Produces:** `openingPreview.ttsVoice` 객체와 미설정·비활성 null 처리.

- [ ] AI first와 USER first의 `ttsVoice` 응답 테스트를 추가한다.
- [ ] 비활성 및 미설정 음성 null 테스트를 추가한다.
- [ ] 테스트를 실행해 기존 `ttsVoiceSetId` 응답으로 인한 실패를 확인한다.
- [ ] 활성 TTS 음성 LEFT JOIN과 응답 매핑을 구현한다.
- [ ] 시나리오 목록 통합 테스트를 통과시킨다.
- [ ] `feat: 시나리오 목록 TTS 음성 응답 변경`으로 커밋한다.

### Task 5: 세션 시작 API 변경

**Files:**
- Modify: `src/main/java/com/landit/landitbe/session/infrastructure/ScenarioSessionStartQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/session/infrastructure/ScenarioSessionStartRow.java`
- Modify: `src/main/java/com/landit/landitbe/session/application/ScenarioSessionStartUseCase.java`
- Modify: `src/main/java/com/landit/landitbe/session/api/dto/SessionStartResponse.java`
- Modify: `src/test/java/com/landit/landitbe/session/ScenarioSessionApiIntegrationTests.java`

**Produces:** 세션 시작 응답의 `ttsVoice` 객체와 미설정·비활성 null 처리.

- [ ] 세션 시작 TTS 음성 응답 테스트를 추가한다.
- [ ] 미설정 및 비활성 음성 null 테스트를 추가한다.
- [ ] 테스트를 실행해 기존 응답과의 차이로 실패하는지 확인한다.
- [ ] 조회 projection과 응답 DTO를 변경한다.
- [ ] 세션 시작 통합 테스트를 통과시킨다.
- [ ] `feat: 세션 시작 TTS 음성 응답 변경`으로 커밋한다.

### Task 6: 전체 회귀 검증

**Files:**
- Modify: `docs/tasks/LAN-100/checklist.md`
- Modify: `docs/tasks/LAN-100/context-notes.md`

- [ ] `git diff --check`를 통과시킨다.
- [ ] `./gradlew test`를 통과시킨다.
- [ ] 마이그레이션, API 문서와 기존 데이터 이전 정책을 재검토한다.
- [ ] 작업 문서의 완료 항목과 검증 결과를 갱신한다.
- [ ] `docs: LAN-100 작업 결과 기록`으로 커밋한다.
