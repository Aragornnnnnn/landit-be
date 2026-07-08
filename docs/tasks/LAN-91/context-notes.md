# LAN-91 시나리오 세션 시작/종료 컨텍스트 노트

## 2026-07-08

- 작업 브랜치는 `feat/LAN-91`이며, 기준 브랜치는 최신 `feat/LAN-62`다.
- SayNow BE 로컬 `develop`이 `origin/develop`보다 6커밋 뒤라 최신 `origin/develop`로 맞춘 뒤 코드를 참고했다.
- SayNow BE는 세션 시작 시 사용자, 시나리오, 잠금 상태를 검증하고 progress row가 없으면 생성한다. Landit도 재시도 처리를 고려해 시작 시 `user_scenario_progress` row를 보장한다.
- Landit의 `learning_session.ai_tutor_id`는 사용자의 `user_profile.ai_tutor_id`를 사용한다.
- Landit은 별도 활성 턴 테이블이 없고 시작 응답에 `messageId`가 필요하므로, 이번 이슈에서는 AI first 첫 메시지를 기존 `session_history`와 `session_history_message`에 저장한다.
- USER first는 AI가 먼저 말하지 않으므로 시작 시 메시지를 저장하지 않고 `userOpeningInstruction`만 반환한다.
- 명세 수정에 따라 진행도 응답은 `minTurnsToGoal`, `maxTurnsToGoal` 대신 `totalQuestionCount`를 내려준다.
- 명세 수정에 따라 TTS 응답 필드는 기존 Landit 스타일과 맞춰 `ttsVoiceSetId`를 사용한다.
- `PATCH /api/v1/sessions/{sessionId}/end`는 `IN_PROGRESS`가 아닌 세션에 대해 `SESSION_ALREADY_COMPLETED` 409로 응답한다.
- 카테고리 `INACTIVE`는 `CATEGORY_LOCKED`, 시나리오 또는 variant `INACTIVE`와 이전 displayOrder 미완료는 `SCENARIO_LOCKED`로 처리한다.
- request body가 없으므로 시작 세션의 `input_mode`는 기본값으로 `MIXED`를 사용한다.
- 새 세션 API는 기존 보안 설정에 명시적으로 추가해 인증 없이는 `AUTH_REQUIRED`로 응답하게 한다.
- `ScenarioSessionApiIntegrationTests`는 구현 전 9개 테스트가 API 부재로 실패했고, 구현 후 AI first, USER first, 인증 실패, 시나리오 없음, 카테고리 잠금, 시나리오 잠금, 정상 중도 종료, 타 사용자 접근, 세션 없음, 이미 종료된 세션 케이스를 통과했다.
- 검증으로 `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`, `./gradlew test`, `git diff --check`를 실행했고 모두 통과했다.
- `user_profile.ai_tutor_id`가 온보딩에서 보장되지 않는 문제는 시나리오 흐름을 별도 이슈로 수정하기로 했으므로 LAN-91에서는 기존 명세대로 필수값 검증만 유지한다.
- 같은 사용자가 같은 시나리오를 동시에 시작하면 `user_scenario_progress`의 `uk_user_scenario_progress` 유니크 제약과 경합할 수 있어, 세션 시작 시 활성 사용자 row를 `PESSIMISTIC_WRITE`로 조회해 진행도 생성 구간을 사용자 단위로 직렬화한다.
- 동시 시작 회귀 테스트는 수정 전 `[500, 201]`로 실패했고, 사용자 row 잠금 적용 후 `[201, 201]`과 진행도 row 1개를 확인했다.
- 세션 시작과 중도 종료는 상태 변경, 여러 저장소 조율, 트랜잭션 경계가 있는 사용자 행동이므로 application 계층 이름을 처음에는 `ScenarioSessionUseCase`로 정리했다.
- 코드 점검 중 새 세션 도메인에 남아 있던 단순 getter 반복은 `@Getter`로 정리하고, `isInProgress()`처럼 의미가 있는 도메인 메서드는 유지한다.
- getter 정리 후 `git diff --check`, `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`, `./gradlew test`를 다시 실행했고 모두 통과했다.
- 리뷰 반영으로 활성 사용자 잠금 조회는 `findActiveByIdForUpdate`로 의미를 고정하고, `ACTIVE` status를 Repository 쿼리 안에 둔다.
- `startedAt`은 세션, 진행도, 히스토리에 같은 시작 시각을 저장하기 위해 UseCase에서 한 번 생성해 각 엔티티 팩토리에 전달하는 방식을 유지한다.
- Swagger Docs Interface 분리는 기존 시나리오 전체 조회 Controller에도 함께 적용해야 하는 문서화 스타일 리팩터링이므로 LAN-91에서는 별도 이슈로 분리한다.
- Repository 직접 의존은 현재 Landit의 가벼운 헥사고날 기준에 맞춰 유지하되, UseCase에 선택 배경을 짧게 주석으로 남긴다.
- `ScenarioSessionStartUseCase`는 세션 시작 흐름이 길어 보이지 않도록 시작 row 조회, 세션 생성, AI 시작 메시지 저장, 응답 조립을 보조 메서드로 분리한다.
- 리뷰 반영 후 `git diff --check`, `./gradlew test --tests com.landit.landitbe.session.ScenarioSessionApiIntegrationTests`, `./gradlew test`를 실행했고 모두 통과했다.
- 세션 시작과 중도 종료는 API 단위와 의존 Repository가 다르므로 `ScenarioSessionStartUseCase`와 `SessionEndUseCase`로 분리한다.
