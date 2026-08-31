# LAN-405 레벨별 콘텐츠 노출 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자 학습 레벨에 맞는 시나리오 질문과 Writing 표현을 노출하고, 진행 중 세션의 질문 그룹을 시작 시점에 고정한다.

**Architecture:** `ContentLearningLevel` 하나가 프로필 레벨을 질문 그룹과 표현 최대 난이도로 해석한다. 질문과 시나리오 세션에는 그룹을 저장하고, 표현은 기존 `difficulty_level`을 그대로 필터에 사용한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL, H2, JUnit 5.

**Spec:** `docs/tasks/LAN-405/design.md`

## 범위

- 질문 그룹은 `LEVEL_1`, `LEVEL_2_TO_3`, `LEVEL_4_TO_5`다.
- `learning_level = null`은 `LEVEL_4_TO_5`와 표현 최대 난이도 5로 처리한다.
- 레벨 1~3은 `difficulty_level <= 3`, 레벨 4~5는 `<= 5` 표현을 사용한다.
- 기존 질문과 시나리오 세션은 `LEVEL_4_TO_5`로 백필한다.
- 신규 질문 240개와 음원은 별도 데이터 PR 범위다.
- 공개 API 응답 필드는 변경하지 않는다.

---

### Task 1: 레벨 정책과 도메인 필드

**Files:**
- Create: `src/main/java/com/landit/landitbe/feature/content/domain/ContentLearningLevel.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/domain/ScenarioQuestion.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/domain/ScenarioSession.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/domain/ContentLearningLevelTest.java`

- [x] `ContentLearningLevel.from(Integer)`와 `maximumExpressionDifficulty()` 테스트를 먼저 실패시킨다.
- [x] 세 질문 그룹과 표현 최대 난이도 3·5 매핑을 구현한다.
- [x] 질문과 시나리오 세션 엔티티에 `question_level_group` 필드를 추가한다.
- [x] `./gradlew test --tests '*ContentLearningLevelTest'`를 통과시킨다.
- [x] `867eb344 feat: 콘텐츠 학습 레벨 매핑을 추가한다`로 커밋한다.

### Task 2: 질문 그룹 스키마와 조회 흐름

**Files:**
- Create: `src/main/resources/db/migration/V74__add_scenario_question_level_group.sql`
- Modify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/ScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/DailyScenarioQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/AdminScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/ScenarioQuestionQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/DailyScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/AdminScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ScenarioContentService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionStartQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionMessageQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/projection/ScenarioSessionMessageContextProjection.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionStartService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/SubmittedMessageService.java`

- [x] DB 테스트에 두 `question_level_group` 컬럼, 체크 제약, 새 질문 유일 제약, 기존 행 백필을 추가해 실패를 확인한다.
- [x] V74에서 질문·시나리오 세션 컬럼을 추가하고 기존 행을 `LEVEL_4_TO_5`로 백필한다.
- [x] 질문 유일 제약을 `(scenario_id, question_level_group, display_order)`로 바꾼다.
- [x] 목록·일일·관리자·세션 시작 쿼리에 `questionLevelGroup` 파라미터와 동일 그룹 조건을 추가한다.
- [x] 세션 시작 시 프로필에서 계산한 그룹을 `ScenarioSession.start(...)`에 저장한다.
- [x] 메시지 컨텍스트에 세션 그룹을 포함하고 후속 질문 조회에 전달한다.
- [x] 저장소 통합 테스트에서 같은 시나리오·순서의 다른 그룹 질문을 구분해 조회하는지 검증한다.
- [x] 세션 통합 테스트에서 프로필 레벨을 중간에 바꿔도 시작 그룹의 후속 질문을 반환하는지 검증한다.
- [x] 다음 검증을 통과시킨다.

```bash
./gradlew test --tests '*DatabaseSchemaIntegrationTests' \
  --tests '*ScenarioQuestionQueryRepositoryIntegrationTests' \
  --tests '*ScenarioSessionApiIntegrationTests'
```

### Task 3: Writing 표현 난이도 필터

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/WritingExpressionRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ExpressionQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ExpressionLearningCompletionService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/notification/service/NotificationTargetPageQueryService.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionQueryServiceTest.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionLearningCompletionServiceTest.java`
- Test: `src/test/java/com/landit/landitbe/feature/notification/service/NotificationTargetPageQueryServiceIntegrationTests.java`

- [ ] 시나리오 표현 목록 조회에 `difficultyLevel <= maximumDifficulty` 조건을 추가한다.
- [ ] 목록과 진행도는 현재 사용자 레벨의 최대 난이도를 같은 repository 메서드에 전달한다.
- [ ] 상세·추가 연습은 시나리오 표현의 난이도가 사용자 한도를 넘으면 `RESOURCE_NOT_FOUND`로 처리한다.
- [ ] 완료 처리는 같은 난이도 검증과 필터된 학습 순서를 사용한다.
- [ ] FreeTalk 표현 조회·완료에는 시나리오 난이도 필터를 적용하지 않는다.
- [ ] 알림 집계 SQL에 `coalesce(up.learning_level, 5)` 기준 최대 난이도 조건을 한 줄 추가한다.
- [ ] 난이도 2·3·4 fixture로 레벨 1~3과 4~5·`null`의 목록·진행도·완료 차이를 검증한다.
- [ ] 다음 검증을 통과시킨다.

```bash
./gradlew test --tests '*ExpressionQueryServiceTest' \
  --tests '*ExpressionLearningCompletionServiceTest' \
  --tests '*NotificationTargetPageQueryServiceIntegrationTests'
```

### Task 4: 전체 검증과 기능 PR 준비

- [ ] `./gradlew check`를 통과시킨다.
- [ ] `git diff origin/develop...HEAD --check`를 통과시킨다.
- [ ] diff에 `expression_level_group`, 신규 질문 데이터, 음원 URL이 없는지 확인한다.
- [ ] 변경 성격별 논리 커밋을 남기고 작업 트리가 깨끗한지 확인한다.
- [ ] `develop` 대상 기능 PR에 기존 feature label과 assignee를 지정한다.
