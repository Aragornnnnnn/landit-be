# LAN-405 Level-Aware Scenario Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자의 학습 레벨에 따라 시나리오 질문과 Writing 표현을 노출하고, 진행 중인 시나리오 세션의 질문 레벨을 시작 시점에 고정한다.

**Architecture:** 질문은 `scenario_question.question_level_group`으로 `LEVEL_1`, `LEVEL_2_TO_3`, `LEVEL_4_TO_5`를 구분한다. 프로필의 1~5 레벨은 공통 `ContentLearningLevel` 정책 객체가 질문 그룹과 표현 최대 난이도로 해석하고, 시나리오 세션은 해석된 질문 그룹을 스냅샷한다. Writing 표현은 이미 존재하는 `difficulty_level`을 조회 조건에 연결하며 새 레벨 필드는 추가하지 않는다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL, H2, JUnit 5, Mockito.

**Spec:** `docs/tasks/LAN-405/design.md`

## Global Constraints

- `user_profile.learning_level` 값은 변경하지 않고 `1`, `2`, `3`, `4`, `5`, `null`을 입력으로 사용한다.
- 질문 그룹은 `LEVEL_1`, `LEVEL_2_TO_3`, `LEVEL_4_TO_5`만 허용한다.
- 표현은 기존 `writing_expression.difficulty_level`만 사용하며 사용자 레벨 1~3은 `<= 3`, 레벨 4~5와 `null`은 `<= 5`를 노출한다.
- 기존 질문·표현·진행 중 시나리오 세션은 `LEVEL_4_TO_5` 호환 동작을 유지한다.
- 신규 질문 240개와 음원은 이 기능 PR에 추가하지 않는다.
- 새 소스 파일은 첫 줄에 역할을 설명하는 한국어 주석을 둔다.
- 코드 변경 후 최소 검증 명령은 `./gradlew check`다.

---

### Task 1: 학습 레벨 정책 객체와 도메인 필드

**Files:**
- Create: `src/main/java/com/landit/landitbe/feature/content/domain/ContentLearningLevel.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/domain/ScenarioQuestion.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/domain/ScenarioSession.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/domain/ContentLearningLevelTest.java`

**Interfaces:**
- Consumes: `Integer userProfile.learningLevel`.
- Produces: `ContentLearningLevel.from(Integer)`, `questionLevelGroup()`, `maximumExpressionDifficulty()`.

- [ ] **Step 1: 매핑 실패 테스트 작성**

```java
@Test
void mapsNullAndLevelsToQuestionGroupsAndExpressionMaximums() {
  assertAll(
      () -> assertEquals(LEVEL_1, ContentLearningLevel.from(1)),
      () -> assertEquals(LEVEL_2_TO_3, ContentLearningLevel.from(2)),
      () -> assertEquals(LEVEL_2_TO_3, ContentLearningLevel.from(3)),
      () -> assertEquals(LEVEL_4_TO_5, ContentLearningLevel.from(4)),
      () -> assertEquals(LEVEL_4_TO_5, ContentLearningLevel.from(5)),
      () -> assertEquals(LEVEL_4_TO_5, ContentLearningLevel.from(null)),
      () -> assertEquals(3, ContentLearningLevel.from(1).maximumExpressionDifficulty()),
      () -> assertEquals(3, ContentLearningLevel.from(3).maximumExpressionDifficulty()),
      () -> assertEquals(5, ContentLearningLevel.from(4).maximumExpressionDifficulty()));
}
```

- [ ] **Step 2: 실패를 확인**

Run: `./gradlew test --tests '*ContentLearningLevelTest'`

Expected: `ContentLearningLevel` 또는 매핑 메서드가 없어 컴파일 또는 테스트가 실패한다.

- [ ] **Step 3: 최소 정책 구현**

`ContentLearningLevel` enum을 `LEVEL_1`, `LEVEL_2_TO_3`, `LEVEL_4_TO_5`로 만들고 다음 계약을 구현한다.

```java
public static ContentLearningLevel from(Integer userLearningLevel);
public int maximumExpressionDifficulty();
```

`null`은 `LEVEL_4_TO_5`, `1~3`은 각각 정해진 그룹, `4~5`는 `LEVEL_4_TO_5`를 반환한다. 범위를 벗어난 값은 `IllegalArgumentException`으로 거절한다.

- [ ] **Step 4: 도메인 필드 연결**

`ScenarioQuestion`에 `questionLevelGroup` enum 필드를, `ScenarioSession`에 동일 enum 필드를 추가한다. `ScenarioSession.start(...)`는 새 그룹 인자를 받아 저장하며 호출부가 컴파일되도록 기존 호출을 모두 갱신한다.

- [ ] **Step 5: 정책 테스트 통과 확인**

Run: `./gradlew test --tests '*ContentLearningLevelTest'`

Expected: PASS.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/landit/landitbe/feature/content/domain/ContentLearningLevel.java src/main/java/com/landit/landitbe/feature/content/domain/ScenarioQuestion.java src/main/java/com/landit/landitbe/feature/session/domain/ScenarioSession.java src/test/java/com/landit/landitbe/feature/content/domain/ContentLearningLevelTest.java
git commit -m "feat: 콘텐츠 학습 레벨 매핑을 추가한다"
```

### Task 2: 질문·세션 스키마와 기존 데이터 백필

**Files:**
- Create: `src/main/resources/db/migration/V74__add_scenario_question_level_group.sql`
- Modify: `src/main/java/com/landit/landitbe/feature/content/domain/ScenarioQuestion.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/domain/ScenarioSession.java`
- Modify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`

**Interfaces:**
- Consumes: Task 1 `ContentLearningLevel` enum.
- Produces: non-null question/session group columns and unique key `(scenario_id, question_level_group, display_order)`.

- [ ] **Step 1: 스키마 기대값 테스트 추가**

`DatabaseSchemaIntegrationTests`에 `scenario_question.question_level_group`, `scenario_session.question_level_group`, 허용 값 체크 제약, 새 유일 제약, 기존 행의 `LEVEL_4_TO_5` 백필을 검증하는 테스트를 추가한다.

- [ ] **Step 2: 마이그레이션 전 실패 확인**

Run: `./gradlew test --tests '*DatabaseSchemaIntegrationTests'`

Expected: 새 컬럼과 제약 기대값이 없어 실패한다.

- [ ] **Step 3: Flyway 마이그레이션 작성**

`V74__add_scenario_question_level_group.sql`에서 다음 순서로 실행한다.

```sql
ALTER TABLE scenario_question ADD COLUMN question_level_group VARCHAR(30);
UPDATE scenario_question SET question_level_group = 'LEVEL_4_TO_5';
ALTER TABLE scenario_question ALTER COLUMN question_level_group SET NOT NULL;
ALTER TABLE scenario_question DROP CONSTRAINT uk_scenario_question_scenario_order;
ALTER TABLE scenario_question ADD CONSTRAINT uk_scenario_question_scenario_level_order
    UNIQUE (scenario_id, question_level_group, display_order);
ALTER TABLE scenario_question ADD CONSTRAINT chk_scenario_question_level_group
    CHECK (question_level_group IN ('LEVEL_1', 'LEVEL_2_TO_3', 'LEVEL_4_TO_5'));

ALTER TABLE scenario_session ADD COLUMN question_level_group VARCHAR(30);
UPDATE scenario_session SET question_level_group = 'LEVEL_4_TO_5';
ALTER TABLE scenario_session ALTER COLUMN question_level_group SET NOT NULL;
ALTER TABLE scenario_session ADD CONSTRAINT chk_scenario_session_level_group
    CHECK (question_level_group IN ('LEVEL_1', 'LEVEL_2_TO_3', 'LEVEL_4_TO_5'));
```

- [ ] **Step 4: 마이그레이션 테스트 통과 확인**

Run: `./gradlew test --tests '*DatabaseSchemaIntegrationTests'`

Expected: PASS with both H2 and the configured migration locations.

- [ ] **Step 5: 커밋**

```bash
git add src/main/resources/db/migration/V74__add_scenario_question_level_group.sql src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java
git commit -m "feat: 시나리오 질문 레벨 그룹 스키마를 추가한다"
```

### Task 3: 첫 질문 조회에 사용자 레벨 적용

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/ScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/DailyScenarioQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/AdminScenarioListQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/DailyScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/AdminScenarioQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionStartQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionStartService.java`
- Test: existing scenario list, daily scenario, admin scenario, and session-start integration tests.

**Interfaces:**
- Consumes: `ContentLearningLevel.questionLevelGroup()` and `ScenarioQuestion.questionLevelGroup`.
- Produces: repository methods receiving `ContentLearningLevel questionLevelGroup` and selecting only that group’s opening question.

- [ ] **Step 1: 레벨별 첫 질문 회귀 테스트 작성**

각 API fixture에 동일 시나리오·순서의 `LEVEL_1`과 `LEVEL_4_TO_5` 질문을 함께 넣고, 프로필 레벨 1·3·5·`null`에서 응답 문구가 각각 기대 그룹으로 선택되는 테스트를 추가한다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*ScenarioListApiIntegrationTests' --tests '*DailyScenarioApiIntegrationTests' --tests '*ScenarioSessionApiIntegrationTests'`

Expected: 현재 쿼리가 레벨 조건을 사용하지 않아 모든 fixture에서 기존 질문을 반환한다.

- [ ] **Step 3: Repository 파라미터와 JPQL 조건 추가**

각 첫 질문 조회 메서드에 `ContentLearningLevel questionLevelGroup` 파라미터를 추가하고 JPQL에 다음 조건을 추가한다.

```sql
AND openingQuestion.questionLevelGroup = :questionLevelGroup
```

세션 시작 쿼리는 `ScenarioSessionStartService`가 잠금 조회한 `UserProfile`에서 그룹을 계산해 전달한다. 목록·일일·관리자 서비스도 같은 정책을 사용해 repository에 전달한다.

- [ ] **Step 4: 기존 null 호환 테스트 통과**

`learning_level = null` fixture가 `LEVEL_4_TO_5` 질문을 반환하는지 확인하고, 각 대상 테스트를 다시 실행한다.

Run: `./gradlew test --tests '*ScenarioListApiIntegrationTests' --tests '*DailyScenarioApiIntegrationTests' --tests '*ScenarioSessionApiIntegrationTests'`

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/landit/landitbe/feature/content/repository/ScenarioListQueryRepository.java src/main/java/com/landit/landitbe/feature/content/repository/DailyScenarioQueryRepository.java src/main/java/com/landit/landitbe/feature/content/repository/AdminScenarioListQueryRepository.java src/main/java/com/landit/landitbe/feature/content/service/ScenarioQueryService.java src/main/java/com/landit/landitbe/feature/content/service/DailyScenarioQueryService.java src/main/java/com/landit/landitbe/feature/content/service/AdminScenarioQueryService.java src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionStartQueryRepository.java src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionService.java src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionStartService.java
git commit -m "feat: 첫 시나리오 질문을 학습 레벨별로 조회한다"
```

### Task 4: 세션 질문 레벨 스냅샷과 후속 질문 조회

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionStartService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionMessageQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/repository/projection/ScenarioSessionMessageContextProjection.java`
- Modify: `src/main/java/com/landit/landitbe/feature/session/service/SubmittedMessageService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ScenarioContentService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/ScenarioQuestionQueryRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/projection/ScenarioQuestionProjection.java`
- Test: `src/test/java/com/landit/landitbe/feature/session/ScenarioSessionApiIntegrationTests.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/ScenarioQuestionQueryRepositoryIntegrationTests.java`

**Interfaces:**
- Consumes: `ScenarioSession.questionLevelGroup`.
- Produces: `ScenarioSessionMessageContextProjection.questionLevelGroup` and `ScenarioContentService.findActiveQuestion(long, int, ContentLearningLevel, Locale, Locale)`.

- [ ] **Step 1: 세션 중 레벨 변경 테스트 작성**

레벨 1로 세션을 시작한 뒤 프로필을 레벨 5로 변경하고 사용자 메시지를 제출해도 다음 고정 질문이 `LEVEL_1` 문구인지 검증한다. 반대 방향과 `null` 시작도 각각 검증한다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests '*ScenarioSessionApiIntegrationTests' --tests '*ScenarioQuestionQueryRepositoryIntegrationTests'`

Expected: 현재 `scenario_session`에 그룹이 없고 후속 조회가 그룹을 전달하지 않아 테스트가 실패한다.

- [ ] **Step 3: 스냅샷 저장과 컨텍스트 전달 구현**

세션 시작 시 `ScenarioSession.start(..., questionLevelGroup)`으로 저장하고, 메시지 컨텍스트 JPQL에 `scenarioSession.questionLevelGroup`을 projection 인자로 추가한다. `SubmittedMessageService`는 projection의 그룹을 `ScenarioContentService.findActiveQuestion`에 전달한다.

- [ ] **Step 4: 후속 질문 쿼리 조건 구현**

`ScenarioQuestionQueryRepository.findActiveQuestion`에 그룹 인자를 추가하고 다음 조건을 사용한다.

```sql
AND scenarioQuestion.questionLevelGroup = :questionLevelGroup
```

- [ ] **Step 5: 세션 테스트 통과 확인**

Run: `./gradlew test --tests '*ScenarioSessionApiIntegrationTests' --tests '*ScenarioQuestionQueryRepositoryIntegrationTests'`

Expected: PASS, 세션 시작 당시 그룹이 후속 질문까지 유지된다.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/landit/landitbe/feature/session/service/ScenarioSessionStartService.java src/main/java/com/landit/landitbe/feature/session/repository/ScenarioSessionMessageQueryRepository.java src/main/java/com/landit/landitbe/feature/session/repository/projection/ScenarioSessionMessageContextProjection.java src/main/java/com/landit/landitbe/feature/session/service/SubmittedMessageService.java src/main/java/com/landit/landitbe/feature/content/service/ScenarioContentService.java src/main/java/com/landit/landitbe/feature/content/repository/ScenarioQuestionQueryRepository.java src/main/java/com/landit/landitbe/feature/content/repository/projection/ScenarioQuestionProjection.java
git commit -m "feat: 시나리오 세션의 질문 레벨을 고정한다"
```

### Task 5: Writing 표현 난이도 필터 적용

**Files:**
- Modify: `src/main/java/com/landit/landitbe/feature/content/repository/WritingExpressionRepository.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ExpressionQueryService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ExpressionLearningCompletionService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/content/service/ExpressionPronunciationService.java`
- Modify: `src/main/java/com/landit/landitbe/feature/notification/service/NotificationTargetPageQueryService.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionQueryServiceTest.java`
- Test: `src/test/java/com/landit/landitbe/feature/content/service/ExpressionLearningCompletionServiceTest.java`
- Test: `src/test/java/com/landit/landitbe/feature/notification/service/NotificationTargetPageQueryServiceIntegrationTests.java`

**Interfaces:**
- Consumes: `ContentLearningLevel.maximumExpressionDifficulty()` and `WritingExpression.difficultyLevel`.
- Produces: filtered scenario-expression reads and direct-access validation without changing FreeTalk behavior.

- [ ] **Step 1: 표현 레벨 필터 테스트 작성**

난이도 2·3·4 표현 fixture를 만들고 레벨 1·3은 2개, 레벨 4·5·`null`은 3개를 반환하는 목록·진행도 테스트를 추가한다. 난이도 4 표현 ID를 레벨 1 사용자가 상세·완료할 수 없음을 테스트한다. FreeTalk expression은 레벨과 무관하게 유지한다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests '*ExpressionQueryServiceTest' --tests '*ExpressionLearningCompletionServiceTest'`

Expected: 현재 repository와 서비스가 모든 활성 시나리오 표현을 반환하거나 직접 ID를 허용해 실패한다.

- [ ] **Step 3: Repository 목록 조건 추가**

시나리오 표현 목록 repository 메서드에 `int maximumDifficulty`를 추가하고 `difficultyLevel <= :maximumDifficulty` 조건을 사용한다. FreeTalk 후보 쿼리에는 이 조건을 넣지 않는다.

- [ ] **Step 4: 서비스에서 사용자 정책 전달**

`ExpressionQueryService`와 `ExpressionLearningCompletionService`가 `UserProfileService`에서 학습 레벨을 읽어 `ContentLearningLevel.from(...).maximumExpressionDifficulty()`를 전달한다. 목록·진행도·순서 잠금은 같은 필터 집합을 공유한다.

- [ ] **Step 5: 직접 접근과 발음 평가 검증 추가**

시나리오 표현 상세·추가 연습·완료·발음 평가가 사용자 허용 난이도보다 높은 표현을 `RESOURCE_NOT_FOUND`로 처리하도록 공통 활성 표현 조회를 보강한다. `FREE_TALK` 표현과 프리톡 세션 연결은 기존 조건만 사용한다.

- [ ] **Step 6: 알림 집계 조건 추가**

`NotificationTargetPageQueryService.loadExpressionRows`의 SQL에 다음 조건을 추가한다.

```sql
and we.difficulty_level <= case
  when coalesce(up.learning_level, 5) <= 3 then 3
  else 5
end
```

- [ ] **Step 7: 표현 테스트 통과 확인**

Run: `./gradlew test --tests '*ExpressionQueryServiceTest' --tests '*ExpressionLearningCompletionServiceTest' --tests '*NotificationTargetPageQueryServiceIntegrationTests'`

Expected: PASS, FreeTalk 회귀와 null 호환 동작도 유지된다.

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/landit/landitbe/feature/content/repository/WritingExpressionRepository.java src/main/java/com/landit/landitbe/feature/content/service/ExpressionQueryService.java src/main/java/com/landit/landitbe/feature/content/service/ExpressionLearningCompletionService.java src/main/java/com/landit/landitbe/feature/content/service/ExpressionPronunciationService.java src/main/java/com/landit/landitbe/feature/notification/service/NotificationTargetPageQueryService.java
git commit -m "feat: 사용자 레벨에 맞는 표현만 노출한다"
```

### Task 6: 통합 검증과 기능 PR 정리

**Files:**
- Modify: affected tests and fixtures only when failures identify a missing level column or method contract.
- Review: `docs/tasks/LAN-405/design.md`

**Interfaces:**
- Consumes: Tasks 1–5 implementation and tests.
- Produces: a clean `feat/LAN-405` feature PR containing no content/audio data migration.

- [ ] **Step 1: 전체 변경 점검**

Run: `git diff origin/develop...HEAD --stat` and `git diff origin/develop...HEAD --check`.

Expected: changes are limited to level policy, question/session schema and queries, expression filtering, tests, and task documents.

- [ ] **Step 2: 전체 테스트 실행**

Run: `./gradlew check`

Expected: Spotless, Checkstyle, database migrations, unit tests, and integration tests all PASS.

- [ ] **Step 3: 계획·설계 충돌 확인**

Check that no code adds `expression_level_group`, no new question/audio content is inserted, and `learning_level = null` remains advanced-compatible.

- [ ] **Step 4: 최종 커밋 정리**

```bash
git status --short --branch
git log --oneline origin/develop..HEAD
```

Expected: the worktree is clean and every commit is one logical change.

- [ ] **Step 5: PR 준비**

Use PR title `feat: LAN-405 레벨별 시나리오 질문과 표현 노출을 지원한다`, target `develop`, apply an existing feature label, assign the developer, and state that the 240-question/audio data PR is intentionally separate.
