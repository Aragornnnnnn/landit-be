# 105 작업 기록

## 결정 사항

- 이 작업은 `origin/develop` 기준 `feat/105` 브랜치에서 진행한다.
- `feat/LAN-62` 이후 브랜치에 이미 `V9__change_star_rating_to_decimal.sql`가 있으므로, 고정 질문 추가 migration은 `V10__add_scenario_questions.sql`로 작성한다.
- 실제 머지 순서는 `V9`가 포함된 LAN-62가 먼저 develop에 들어간 뒤, 이 브랜치의 `V10`이 들어가는 흐름이 안전하다.
- 기존 `scenario_language_variant.ai_opening_message`는 AI first 시작 메시지로 유지한다.
- 새 고정 질문 테이블은 사용자 발화 이후 다음 질문 생성을 위한 콘텐츠 모델로 둔다.
- DB 컬럼명은 기존 컨벤션에 맞춰 `display_order`를 사용하고, AI 요청 DTO에서는 `sequence`로 매핑한다.

## 검증 기준

- Flyway 적용 후 `scenario_question`, `scenario_question_language_variant` 테이블이 존재해야 한다.
- 고정 질문은 `scenario_id + display_order`로 순서 중복을 막아야 한다.
- 질문 문구는 `scenario_question_id + target_locale + base_locale` 조합으로 중복을 막아야 한다.
- 세션 메시지 제출 API가 사용할 수 있도록 `scenarioId + displayOrder + targetLocale + baseLocale + ACTIVE status` 기준으로 질문을 조회할 수 있어야 한다.

## 검증 기록

- RED: `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests --tests com.landit.landitbe.content.ScenarioQuestionQueryRepositoryIntegrationTests` 실행 시 `ScenarioQuestionQueryRepository`와 `ScenarioQuestionRow`가 없어 컴파일 실패했다.
- GREEN: 동일 명령 재실행 결과 통과했다.
- 전체 검증: `./gradlew test` 통과했다.
