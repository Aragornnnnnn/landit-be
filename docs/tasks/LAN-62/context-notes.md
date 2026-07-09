# LAN-62 시나리오 목록 API 컨텍스트 노트

## 2026-07-08

- 사용자가 Notion 이슈 번호 `LAN-62`에 해당하는 작업 브랜치 생성을 요청해 `origin/develop` 기준 `feat/LAN-62`에서 작업한다.
- `GET /api/v1/scenarios`는 인증된 사용자별 완료 여부와 별점을 함께 내려야 하므로 `AuthUserPrincipal.userId()`를 기준으로 조회한다.
- 명세에는 locale query parameter가 없으므로 이번 API는 `user_profile`의 `target_locale`, `base_locale` 조합으로 조회한다.
- 목록 조회는 Entity getter를 넓히지 않고 JPA projection query로 필요한 필드만 읽는다.
- 별점은 DB와 API 모두 FE 별점 스케일인 1.0, 1.5, 2.0, 2.5, 3.0을 그대로 사용한다.
- `category`, `scenario`, `scenario_language_variant`의 `status`는 활성/비활성 여부로만 보고, 시나리오 순차 잠금은 사용자 진행도 기준으로 계산한다.
- 잠긴 시나리오는 `openingPreview`를 만들지 않는다.
- AI first preview는 AI 시작 메시지, 번역, 속마음, 속마음 유형을 사용하고 USER first preview는 사용자 시작 안내만 사용한다.
- Swagger 문서는 bearer security scheme과 `GET /api/v1/scenarios` operation/response DTO schema annotation으로 보강한다.
- 목록 조회 API는 세션이나 메시지를 저장하지 않는다.
- 명세의 `INVALID_TOKEN`을 맞추기 위해 잘못된 Bearer access token은 공통 인증 경로에서 `INVALID_TOKEN`으로 응답하도록 정리한다.
- `ScenarioListApiIntegrationTests`는 구현 전 `GET /api/v1/scenarios` 인증 요구, `INVALID_TOKEN`, 목록 응답 기대값에서 실패했고, API와 공통 인증 매핑 구현 후 통과했다.
- 관련 검증으로 `./gradlew test --tests 'com.landit.landitbe.content.ScenarioListApiIntegrationTests'`, `./gradlew test --tests 'com.landit.landitbe.auth.SocialAuthApiIntegrationTests'`, `git diff --check`, `./gradlew test`를 실행했고 모두 통과했다.
- 커밋은 `CONTRIBUTING.md`의 30줄 내외 권장 규칙을 고려해 12개 논리 단위로 분할한다.
- 병렬 브랜치 충돌을 줄이기 위해 LAN-62 작업 기록은 루트 `checklist.md`, `context-notes.md`가 아니라 `docs/tasks/LAN-62/`로 분리한다.
- 앞으로 새 작업은 `docs/tasks/{ISSUE_NUMBER}/checklist.md`와 `docs/tasks/{ISSUE_NUMBER}/context-notes.md`에 기록하도록 `AGENTS.md`와 `README.md`에 반영한다.
- SayNow BE의 `ScenarioService`는 카테고리 안의 시나리오를 `displayOrder` 순서로 순회하며 첫 시나리오는 열고, 이후 시나리오는 바로 앞 시나리오가 완료되어야 열리도록 계산한다.
- Landit도 같은 규칙을 적용한다. `status`가 `INACTIVE`인 콘텐츠는 사용할 수 없는 상태로 유지하되, 정상 활성 콘텐츠의 잠금 여부는 같은 카테고리 내 이전 `displayOrder` 시나리오의 `CLEARED` 기록으로 판단한다.
- 순차 잠금 사유는 SayNow와 맞춰 `PREVIOUS_SCENARIO_NOT_COMPLETED` 문자열을 사용한다.
- `ScenarioListApiIntegrationTests.scenariosLockNextScenarioUntilPreviousScenarioIsCleared`를 추가해 이전 시나리오 완료 기록이 없으면 다음 시나리오가 잠기고 `openingPreview`가 null이 되는 동작을 고정한다.
- 기존 목록 테스트는 1번 시나리오가 `CLEARED`일 때 2번 시나리오가 열리는 케이스를 계속 검증한다.
- 검증으로 `./gradlew test --tests com.landit.landitbe.content.ScenarioListApiIntegrationTests`, `git diff --check`, `./gradlew test`를 실행했고 모두 통과했다.
- 후속 확인 중 `best_star_rating`을 1~5 정수로 저장하고 API에서 1.0~3.0으로 변환하던 정책이 잘못됐음을 확인했다.
- `V4__apply_dbml_schema.sql`은 이미 develop에 존재하는 migration이라 수정하지 않고 별도 migration을 추가해 `user_scenario_progress.best_star_rating`과 `session_history_summary_feedback.star_rating`을 `NUMERIC(2, 1)`로 변경한다.
- `V10__add_scenario_questions.sql`이 먼저 develop DB에 적용된 상태라 별점 migration은 `V11__change_star_rating_to_decimal.sql`로 번호를 조정한다.
- 현재 DB에는 별점 데이터가 없으므로 `V11` migration은 기존 데이터 변환 없이 컬럼 타입과 constraint만 변경한다.
- 별점 check constraint는 null 또는 `1.0`, `1.5`, `2.0`, `2.5`, `3.0`만 허용한다.
- Java 엔티티와 projection의 별점 타입은 `Integer`가 아니라 `BigDecimal`로 맞춘다.
- 시나리오 목록 응답은 DB 값을 별도 변환하지 않고 그대로 반환한다.

## 2026-07-09

- PR #3은 `feat/LAN-62`에서 `develop`으로 향하고, GitHub 기준 `CONFLICTING` 상태다.
- 최신 `origin/develop`에는 PR #8 `feat/105`, PR #1 `feat/LAN-59`, PR #4 `feat/LAN-104`가 반영됐다.
- 이번 작업의 1차 목표는 `feat/LAN-62`를 최신 `origin/develop` 위로 올려 PR #3 충돌을 없애는 것이다.
- LAN-91 후속 PR 스택은 `feat/LAN-91-base`, `feat/LAN-91-start`, `feat/LAN-91-end` 순서로 `feat/LAN-62` 위에 쌓여 있으므로, LAN-62를 먼저 push한 뒤 아래 브랜치부터 rebase한다.
- PR #9 `feat/LAN-81`은 `feat/LAN-79`를 base로 하는 별도 스택이라 이번 후속 PR rebase 범위에서 제외한다.
- `feat/LAN-62` rebase 중 루트 `checklist.md`, `context-notes.md`는 develop의 LAN-59 과거 로그만 유지하고, LAN-62 작업 로그는 `docs/tasks/LAN-62/`에만 남겼다.
- `AuthSecurityConfig` 충돌은 develop의 `GET /api/v1/expressions/**` 인증 경로와 LAN-62의 `GET /api/v1/scenarios` 인증 경로를 모두 유지해 해결했다.
- 전체 테스트 첫 실행은 `ScenarioListApiIntegrationTests.setUp()`에서 `scenario` 삭제 시 `writing_expression.scenario_id` FK가 남아 실패했다.
- 원인은 LAN-59 표현 API 통합 테스트가 남긴 `writing_expression`, `user_writing_expression_completion` 데이터와 LAN-62 테스트 cleanup 범위가 맞지 않은 것이다.
- `ScenarioListApiIntegrationTests` setup에서 `user_writing_expression_completion`, `writing_expression`을 먼저 삭제하도록 보강했다.
- 검증으로 `git diff --check`, `./gradlew test --tests com.landit.landitbe.content.ScenarioListApiIntegrationTests`, `./gradlew test`를 실행했고 모두 통과했다.
