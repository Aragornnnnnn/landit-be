# LAN-62 시나리오 목록 API 컨텍스트 노트

## 2026-07-08

- 사용자가 Notion 이슈 번호 `LAN-62`에 해당하는 작업 브랜치 생성을 요청해 `origin/develop` 기준 `feat/LAN-62`에서 작업한다.
- `GET /api/v1/scenarios`는 인증된 사용자별 완료 여부와 별점을 함께 내려야 하므로 `AuthUserPrincipal.userId()`를 기준으로 조회한다.
- 명세에는 locale query parameter가 없으므로 이번 API는 `user_profile`의 `target_locale`, `base_locale` 조합으로 조회한다.
- 목록 조회는 Entity getter를 넓히지 않고 JPA projection query로 필요한 필드만 읽는다.
- 별점은 DB의 `best_star_rating` 정수 1~5를 API의 1, 1.5, 2, 2.5, 3 범위로 변환한다.
- 현재 스키마에는 별도 잠금 규칙 테이블이나 잠금 사유 컬럼이 없으므로 `category`, `scenario`, `scenario_language_variant`의 `status`가 `ACTIVE`가 아닌 경우 잠금으로 계산하고 고정 사유를 내려준다.
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
